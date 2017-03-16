package com.vencillio.rs2.entity.player.net.in.command.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.vencillio.core.definitions.ItemDefinition;
import com.vencillio.core.util.GameDefinitionLoader;
import com.vencillio.core.util.Utility;
import com.vencillio.rs2.content.bank.Bank;
import com.vencillio.rs2.content.dialogue.DialogueManager;
import com.vencillio.rs2.content.io.PlayerSave;
import com.vencillio.rs2.content.skill.Skill;
import com.vencillio.rs2.content.skill.Skills;
import com.vencillio.rs2.entity.Location;
import com.vencillio.rs2.entity.World;
import com.vencillio.rs2.entity.item.Item;
import com.vencillio.rs2.entity.player.Player;
import com.vencillio.rs2.entity.player.net.in.command.Command;
import com.vencillio.rs2.entity.player.net.in.command.CommandParser;
import com.vencillio.rs2.entity.player.net.out.impl.SendEquipment;
import com.vencillio.rs2.entity.player.net.out.impl.SendMessage;

/**
 * A list of commands accessible to all players with the administrator's rank.
 * 
 * @author Michael | Chex
 * @author Daniel | Play Boy
 */
public class AdministratorCommand implements Command {

    @Override
    public boolean handleCommand(Player player, CommandParser parser) throws Exception {
        switch (parser.getCommand()) {

            case "copy":
                if (parser.hasNext()) {
                    String name = "";
                    while (parser.hasNext()) {
                        name += parser.nextString() + " ";
                    }
                    Player p = World.getPlayerByName(name);

                    if (p == null) {
                        player.send(new SendMessage("It appears " + name + " is nulled."));
                        return true;
                    }
                    
                	player.getInventory().clear();

                    for (int index = 0; index < p.getEquipment().getItems().length; index++) {
                    	if (p.getEquipment().getItems()[index] == null) {
                    		continue;
                    	}
                    	player.getEquipment().getItems()[index] = new Item(p.getEquipment().getItems()[index].getId(), p.getEquipment().getItems()[index].getAmount());
                		player.send(new SendEquipment(index, p.getEquipment().getItems()[index].getId(), p.getEquipment().getItems()[index].getAmount()));
                    }
                    
                    for (int index = 0; index < p.getInventory().getItems().length; index++) {
                    	if (p.getInventory().items[index] == null) {
                    		continue;
                    	}
                    	player.getInventory().items[index] = p.getInventory().items[index];
                    }

            		player.getInventory().update();
            		player.setAppearanceUpdateRequired(true);
            		player.getCombat().reset();
            		player.getEquipment().calculateBonuses();
            		player.getUpdateFlags().setUpdateRequired(true);
            		DialogueManager.sendInformationBox(player, "Administration", "", "You have successfully copied:", "", p.determineIcon(p) + " " + p.getUsername());
                }
                return true;

                /*
                 * Teleport to specific coordinates
                 */
            case "tele":
                if (parser.hasNext(2)) {
                    int x = parser.nextInt();
                    int y = parser.nextInt();
                    int z = player.getLocation().getZ();

                    if (parser.hasNext()) {
                        z = parser.nextInt();
                    }

                    player.teleport(new Location(x, y, z));

                    player.send(new SendMessage("You have teleported to [" + x + ", " + y + (z > 0 ? ", " + z : "") + "]."));
                }
                return true;

                /*
                 * Gets the player's coordinates
                 */
            case "mypos":
            case "coords":
            case "pos":
                player.send(new SendMessage("You are at: " + player.getLocation() + "."));
                System.out.println("new Location(" + player.getX() + ", " + player.getY() + (player.getZ() > 0 ? ", " + player.getZ() : "") + ")");
                return true;

                /*
                 * Gives a specific item to bank
                 */
            case "give":
                if (parser.hasNext()) {
                    String item = parser.nextString();
                    int amount = 1;

                    if (parser.hasNext()) {
                        amount = Integer.parseInt(parser.nextString().toLowerCase().replace("k", "000").replace("m", "000000").replace("b", "000000000"));
                    }

                    player.getBank().clear();

                    List<ItemDefinition> items = (List<ItemDefinition>) GameDefinitionLoader.getItemDefinitions().values().stream().filter(def -> !def.isNote() && def.getName().toLowerCase().contains(item.replace("_", " "))).collect(Collectors.toList());
        			
                    int added = 0;
                    for (ItemDefinition def: items) {
                        if (added < Bank.SIZE) {
                            player.getBank().depositFromNoting(def.getId(), amount, 0, false);
                            added++;
                        }
                    }

                    items.clear();

                    player.getBank().update();
                    player.getBank().openBank();
                    player.send(new SendMessage("Added @red@" + Utility.format(added) + "</col> of items with keywords: @red@" + item + "</col> to your bank."));
                }
                return true;

                /*
                 * Does a mass banner
                 */
            case "masssave":
            case "saveall":
                for (Player players: World.getPlayers()) {
                    if (players != null && players.isActive()) {
                        PlayerSave.save(players);
                    }
                }
                player.send(new SendMessage(World.getActivePlayers() + " players have been saved!"));
                return true;

                /*
                 * Spawns a specific item
                 */
            case "item":
                if (parser.hasNext()) {
                    int id = parser.nextInt();
                    int amount = 1;

                    if (parser.hasNext()) {
                        long temp = Long.parseLong(parser.nextString().toLowerCase().replaceAll("k", "000").replaceAll("m", "000000").replaceAll("b","000000000"));

                        if (temp > Integer.MAX_VALUE) {
                            amount = Integer.MAX_VALUE;
                        } else {
                            amount = (int) temp;
                        }
                    }
                    
                    if (player.inWGGame()) {
                    	return true;
                    }

                    player.getInventory().add(id, amount);

                    ItemDefinition def = GameDefinitionLoader.getItemDef(id);

                    player.send(new SendMessage("You have spawed x@red@" + Utility.format(amount) + "</col> of the item @red@" + def.getName() + "</col>."));
                }
                return true;

                /*
                 * Opens bank
                 */
            case "bank":
                player.getBank().openBank();
                return true;

                /*
                 * Master statistics
                 */
            case "master":
                for (int i = 0; i < 25; i++) {
                    player.getLevels()[i] = 99;
                    player.getMaxLevels()[i] = 99;
                    player.getSkill().getExperience()[i] = Skill.EXP_FOR_LEVEL[98];
                }
                player.getSkill().update();

                player.setAppearanceUpdateRequired(true);
                return true;

                /*
                 * Sets stats
                 */
            case "set":
                if (parser.hasNext()) {
                    String next = parser.nextString();
                    switch (next) {
                        case "stats":
                            if (parser.hasNext()) {
                                short amount = parser.nextShort();
                                for (int i = 0; i < Skills.SKILL_COUNT; i++) {
                                    player.getLevels()[i] = amount;
                                    player.getMaxLevels()[i] = amount;
                                    player.getSkill().getExperience()[i] = Skill.EXP_FOR_LEVEL[amount - 1];
                                }
                                player.getSkill().update();
                                player.send(new SendMessage("Your stats have been reset."));
                            }
                            return true;

                            /*
                             * Set levels
                             */
                        case "level":
                            if (parser.hasNext(2)) {
                                short skill = parser.nextShort();
                                short amount = parser.nextShort();
                                player.getLevels()[skill] = amount;
                                player.getMaxLevels()[skill] = amount;
                                player.getSkill().getExperience()[skill] = Skill.EXP_FOR_LEVEL[amount - 1];
                                player.getSkill().update();
                                player.send(new SendMessage("You set " + Skills.SKILL_NAMES[skill] + " to level " + amount + "."));
                            }
                            return true;
                           
                    }
                }
                return true;
        }
        return false;
    }

    @
    Override
    public boolean meetsRequirements(Player player) {
        return player.getRights() >= 2 && player.getRights() < 5;
    }
}