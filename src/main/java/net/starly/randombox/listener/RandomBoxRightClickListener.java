package net.starly.randombox.listener;

import net.starly.core.jb.version.nms.tank.NmsItemStackUtil;
import net.starly.core.jb.version.nms.wrapper.ItemStackWrapper;
import net.starly.core.jb.version.nms.wrapper.NBTTagCompoundWrapper;
import net.starly.core.util.InventoryUtil;
import net.starly.randombox.RandomBox;
import net.starly.randombox.message.MessageContext;
import net.starly.randombox.message.MessageType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;

public class RandomBoxRightClickListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null || itemStack.getType() == Material.AIR) return;

        ItemStackWrapper nmsStack = NmsItemStackUtil.getInstance().asNMSCopy(itemStack);
        NBTTagCompoundWrapper tagCompound = nmsStack.getTag();
        if (tagCompound == null) return;

        String boxName = tagCompound.getString("RANDOMBOX_NAME");
        if (boxName == null || boxName.isEmpty()) return;

        event.setCancelled(true);

        ItemStack handStack = player.getInventory().getItemInMainHand();
        if (handStack.getAmount() == 1) player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        else {
            if (InventoryUtil.getSpace(player.getInventory()) - 5 < 1) {
                player.sendMessage(MessageContext.getInstance().getMessageAfterPrefix(MessageType.ERROR, "inventoryIsFull").replace("{target}", player.getName()));
                return;
            }

            handStack.setAmount(handStack.getAmount() - 1);
        }

        net.starly.randombox.randombox.RandomBox randomBox = RandomBox.getInstance().getRandomBoxRepository().getRandomBox(boxName);
        if (randomBox != null) {
            List<ItemStack> items = randomBox.getItems();
            try {
                ItemStack prizeStack = items.get(new Random().nextInt(items.size()));

                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItem(player.getLocation(), prizeStack);
                } else {
                    JavaPlugin plugin = RandomBox.getInstance();
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.getInventory().addItem(prizeStack), 1L);
                }

                player.sendMessage(MessageContext.getInstance().getMessageAfterPrefix(MessageType.NORMAL, "randomBoxOpened")
                        .replace("{item}", (prizeStack.getItemMeta().hasDisplayName() ?
                                prizeStack.getItemMeta().getDisplayName() : prizeStack.getType()) + "x" + prizeStack.getAmount())
                );
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            } catch (IllegalArgumentException e) {
                player.sendMessage(MessageContext.getInstance().getMessageAfterPrefix(MessageType.ERROR, "randomBoxIsEmpty"));
            }
        }
    }
}
