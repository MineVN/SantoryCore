package mk.plugin.santory.listener;

import com.google.common.collect.Lists;
import mk.plugin.santory.config.Configs;
import mk.plugin.santory.slave.master.Masters;
import mk.plugin.santory.wish.Wish;
import mk.plugin.santory.wish.WishKey;
import mk.plugin.santory.wish.WishRolls;
import mk.plugin.santory.wish.Wishes;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import mk.plugin.santory.damage.Damage;
import mk.plugin.santory.damage.DamageType;
import mk.plugin.santory.damage.Damages;
import mk.plugin.santory.traveler.Travelers;
import mk.plugin.santory.utils.Utils;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerListener implements Listener {

	/*
	Keep Stone
	 */
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		e.setKeepInventory(true);
		e.setKeepLevel(true);

		var player = e.getEntity();
		if (player.hasPermission("santorycore.admin")) return;

		for (ItemStack is : player.getInventory().getContents()) {
			if (Configs.isKeepStone(is)) {
				is.setAmount(is.getAmount() - 1);
				player.sendMessage("");
				player.sendMessage("§aGiữ đồ khi chết, tiêu thụ 1 Đá bảo hộ");
				player.sendMessage("");
				return;
			}
		}

		e.setKeepInventory(false);
		player.sendMessage("");
		player.sendMessage("§cKhông có Đá bảo hộ, rơi đồ khi chết");
		player.sendMessage("");
	}


	/*
	Crate hit
	 */
	@EventHandler
	public void onCrateInteract(PlayerInteractEvent e) {
		Block b = e.getClickedBlock();
		if (b == null) return;
		if (e.getHand() != EquipmentSlot.HAND) return;

		var player = e.getPlayer();
		for (Map.Entry<String, Wish> entry : Configs.getWishes().entrySet()) {
			for (var ld : entry.getValue().getLocations()) {
				Block bld = ld.toBukkitLocation().getBlock();
				if (bld.getWorld() == b.getWorld() && bld.getX() == b.getX() && bld.getY() == b.getY() && bld.getZ() == b.getZ()) {
					e.setCancelled(true);
					String id = entry.getKey();

					// Right crate, key
					var is = player.getInventory().getItemInMainHand();
					var keyID = Wishes.keyFrom(is);
					if (keyID == null) {
						player.sendMessage("§cPhải cầm chìa để mở hòm!");
						return;
					}

					// Match create + key
					var wk = Configs.getWishKey(keyID);
					if (wk.getWishes().contains(id)) {
						is.setAmount(is.getAmount() - 1);
						player.updateInventory();
						WishRolls.roll(entry.getValue(), player);
					}
					else {
						player.sendMessage("§cChìa không khớp với hòm!");
					}
				}
			}
		}
	}

	// Tab tag
	@EventHandler
	public void onTabComplete(TabCompleteEvent e) {
		String s = e.getBuffer();
		if (s.contains("@")) {
			String regex = "@(?<preName>\\S+)";

			Pattern pt = Pattern.compile(regex);
			Matcher m = pt.matcher(s);

			String preName = null;
			while (m.find()) {
				preName = m.group("preName");
			}
			if (preName == null) return;

			List<String> avl = Lists.newArrayList();
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.getName().toLowerCase().startsWith(preName.toLowerCase())) {
					avl.add("@" + p.getName());
				}
			}

			e.setCompletions(avl);
		}
	}


	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Travelers.saveAndClearCache(e.getPlayer().getName());
		Masters.saveAndClearCache(e.getPlayer());
	}
	
	@EventHandler
	public void onHitMuaTen(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Arrow) {
			Arrow a = (Arrow) e.getDamager();
			if (a.hasMetadata("arrow.MuaTen")) {
				Player player = (Player) a.getShooter();
				a.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, a.getLocation(), 1, 0, 0, 0, 0);
				a.getWorld().playSound(a.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
				double damage = a.getMetadata("arrow.MuaTen").get(0).asDouble();
				Utils.getLivingEntities(player, a.getLocation(), 2, 2, 2).forEach(le -> {
					if (!Utils.canAttack(le)) return;
					Damages.damage(player, le, new Damage(damage, DamageType.SKILL), 5);
				});
			}
		}
	}
	
}
