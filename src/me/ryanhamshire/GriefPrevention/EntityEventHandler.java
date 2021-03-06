/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import me.ryanhamshire.GriefPrevention.events.CombatStartedEvent;
import me.ryanhamshire.GriefPrevention.events.PreventPvPEvent;
import me.ryanhamshire.GriefPrevention.events.ProtectDeathDropsEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WaterMob;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

//handles events related to entities
public class EntityEventHandler implements Listener
{
	//convenience reference for the singleton datastore
	private DataStore dataStore;
	
	public EntityEventHandler(DataStore dataStore)
	{
		this.dataStore = dataStore;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityFormBlock(EntityBlockFormEvent event)
	{
	    Entity entity = event.getEntity();
	    if(entity.getType() == EntityType.PLAYER)
        {
            Player player = (Player)event.getEntity();
            String noBuildReason = GriefPrevention.instance.allowBuild(player, event.getBlock().getLocation(), event.getNewState().getType());
            if(noBuildReason != null)
            {
                event.setCancelled(true);
            }
        }
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityChangeBlock(EntityChangeBlockEvent event)
	{
		if(event.getEntity() instanceof Arrow){
        	Arrow arrow = (Arrow) event.getEntity();
        	if(arrow.getShooter() instanceof Player){
        		Player player = (Player) arrow.getShooter();
        		Block b = event.getBlock();
        		
                if(GriefPrevention.instance.allowBreak(player, b, b.getLocation()) != null)
                {
                    event.setCancelled(true);
                }
        	}
        }
		
		
		else if(!GriefPrevention.instance.config_endermenMoveBlocks && event.getEntityType() == EntityType.ENDERMAN)
		{
			event.setCancelled(true);
		}
		
		else if(!GriefPrevention.instance.config_silverfishBreakBlocks && event.getEntityType() == EntityType.SILVERFISH)
		{
			event.setCancelled(true);
		}
	    
		else if(!GriefPrevention.instance.config_rabbitsEatCrops && event.getEntityType() == EntityType.RABBIT)
        {
            event.setCancelled(true);
        }
		
		//don't allow the wither to break blocks, when the wither is determined, too expensive to constantly check for claimed blocks
		else if(event.getEntityType() == EntityType.WITHER && GriefPrevention.instance.config_claims_worldModes.get(event.getBlock().getWorld()) != ClaimsMode.Disabled)
		{
			event.setCancelled(true);
		}
	    
	    //don't allow crops to be trampled, except by a player with build permission
		else if(event.getTo() == Material.DIRT && event.getBlock().getType() == Material.SOIL)
		{
		    if(event.getEntityType() != EntityType.PLAYER)
		    {
		        event.setCancelled(true);
		    }
		    else
		    {
		        Player player = (Player)event.getEntity();
		        Block block = event.getBlock();
		        if(GriefPrevention.instance.allowBreak(player, block, block.getLocation()) != null)
		        {
		            event.setCancelled(true);
		        }
		    }
		}
		
		//sand cannon fix - when the falling block doesn't fall straight down, take additional anti-grief steps
		else if (event.getEntityType() == EntityType.FALLING_BLOCK)
		{
		    FallingBlock entity = (FallingBlock)event.getEntity();
		    Block block = event.getBlock();
		    
		    //if changing a block TO air, this is when the falling block formed.  note its original location
		    if(event.getTo() == Material.AIR)
		    {
		        entity.setMetadata("GP_FALLINGBLOCK", new FixedMetadataValue(GriefPrevention.instance, block.getLocation()));
		    }
		    //otherwise, the falling block is forming a block.  compare new location to original source
		    else
		    {
		         List<MetadataValue> values = entity.getMetadata("GP_FALLINGBLOCK");
				 //If entity fell through an end portal, allow it to form, as the event is erroneously fired twice in this scenario.
				 if (entity.hasMetadata("GP_FELLTHROUGHPORTAL")) return;
		         //if we're not sure where this entity came from (maybe another plugin didn't follow the standard?), allow the block to form
		         if(values.size() < 1) return;
		         
		         Location originalLocation = (Location)(values.get(0).value());
		         Location newLocation = block.getLocation();
		         
		         //if did not fall straight down
		         if(originalLocation.getBlockX() != newLocation.getBlockX() || originalLocation.getBlockZ() != newLocation.getBlockZ())
		         {
		             //in creative mode worlds, never form the block
		             if(GriefPrevention.instance.config_claims_worldModes.get(newLocation.getWorld()) == ClaimsMode.Creative)
		             {
		                 event.setCancelled(true);
		                 return;
		             }
		             
		             //in other worlds, if landing in land claim, only allow if source was also in the land claim
		             Claim claim = this.dataStore.getClaimAt(newLocation, false, null);
		             if(claim != null && !claim.contains(originalLocation, false, false))
		             {
		                 //when not allowed, drop as item instead of forming a block
		                 event.setCancelled(true);
		                 @SuppressWarnings("deprecation")
		                 ItemStack itemStack = new ItemStack(entity.getMaterial(), 1, entity.getBlockData());
		                 Item item = block.getWorld().dropItem(entity.getLocation(), itemStack);
		                 item.setVelocity(new Vector());
		             }
		         }
		    }
		}
	}

	//Used by "sand cannon" fix to ignore fallingblocks that fell through End Portals
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onFallingBlockEnterPortal(EntityPortalEnterEvent event)
	{
		if (event.getEntityType() != EntityType.FALLING_BLOCK)
			return;
		event.getEntity().setMetadata("GP_FELLTHROUGHPORTAL", new FixedMetadataValue(GriefPrevention.instance, true));
	}
	
	//don't allow zombies to break down doors
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onZombieBreakDoor(EntityBreakDoorEvent event)
	{		
		if(!GriefPrevention.instance.config_zombiesBreakDoors) event.setCancelled(true);
	}
	
	//don't allow entities to trample crops, or arrows to activate buttons
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityInteract(EntityInteractEvent event)
	{
		Material material = event.getBlock().getType();
	    if(material == Material.SOIL)
	    {
	        if(!GriefPrevention.instance.config_creaturesTrampleCrops)
	        {
	            event.setCancelled(true);
	        }
	        else
	        {
	            Entity rider = event.getEntity().getPassenger();
	            if(rider != null && rider.getType() == EntityType.PLAYER)
	            {
	                event.setCancelled(true);
	            }
	        }
		}
		
		if(event.getEntity() instanceof Arrow && event.getBlock().getType() == Material.WOOD_BUTTON)
		{
			Arrow arrow = (Arrow)event.getEntity();
			if(arrow.getShooter() instanceof Player)
			{
				Player shooter = (Player) arrow.getShooter();
				Claim claim = this.dataStore.getClaimAt(event.getBlock().getLocation(), false, null);
				if (claim != null)
				{
					String noAccessReason = claim.allowAccess(shooter);
					if(noAccessReason != null)
					{
						event.setCancelled(true);
						GriefPrevention.sendMessage(shooter, TextMode.Err, noAccessReason);
						return;
					}
				}
			}
		}	 
	}
	
	//when an entity explodes...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent explodeEvent)
	{		
		this.handleExplosion(explodeEvent.getLocation(), explodeEvent.getEntity(), explodeEvent.blockList());
	}
	
	//when a block explodes...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent explodeEvent)
    {       
        this.handleExplosion(explodeEvent.getBlock().getLocation(), null, explodeEvent.blockList());
    }
    
    @SuppressWarnings("deprecation")
    void handleExplosion(Location location, Entity entity, List<Block> blocks)
    {
        //only applies to claims-enabled worlds
        World world = location.getWorld();
        
        if(!GriefPrevention.instance.claimsEnabledForWorld(world)) return;
        
        //FEATURE: explosions don't destroy surface blocks by default
        boolean isCreeper = (entity != null && entity instanceof Creeper);
        
        boolean applySurfaceRules = world.getEnvironment() == Environment.NORMAL && ((isCreeper && GriefPrevention.instance.config_blockSurfaceCreeperExplosions) || (!isCreeper && GriefPrevention.instance.config_blockSurfaceOtherExplosions));
        
        //special rule for creative worlds: explosions don't destroy anything
        if(GriefPrevention.instance.creativeRulesApply(location))
        {
            for(int i = 0; i < blocks.size(); i++)
            {
                Block block = blocks.get(i);
                if(GriefPrevention.instance.config_mods_explodableIds.Contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
                
                blocks.remove(i--);
            }
            
            return;
        }
        
        //make a list of blocks which were allowed to explode
        List<Block> explodedBlocks = new ArrayList<Block>();
        Claim cachedClaim = null;
        for(int i = 0; i < blocks.size(); i++)
        {
            Block block = blocks.get(i);
            
            //always ignore air blocks
            if(block.getType() == Material.AIR) continue;
            
            //always allow certain block types to explode
            if(GriefPrevention.instance.config_mods_explodableIds.Contains(new MaterialInfo(block.getTypeId(), block.getData(), null)))
            {
                explodedBlocks.add(block);
                continue;
            }
            
            //is it in a land claim?
            Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, cachedClaim);
            if(claim != null)
            {
                cachedClaim = claim;
            }
                    
            //if yes, apply claim exemptions if they should apply
            if(claim != null && (claim.areExplosivesAllowed || !GriefPrevention.instance.config_blockClaimExplosions))
            {
                explodedBlocks.add(block);
                continue;
            }
            
            //if claim is under siege, allow soft blocks to be destroyed
            if(claim != null && claim.siegeData != null)
            {
                Material material = block.getType();
                boolean breakable = false;
                for(int j = 0; j < GriefPrevention.instance.config_siege_blocks.size(); j++)
                {
                    Material breakableMaterial = GriefPrevention.instance.config_siege_blocks.get(j);
                    if(breakableMaterial == material)
                    {
                        breakable = true;
                        explodedBlocks.add(block);
                        break;
                    }
                }

                if(breakable) continue;
            }
            
            //if no, then also consider surface rules
            if(claim == null)
            {
                if(!applySurfaceRules || block.getLocation().getBlockY() < GriefPrevention.instance.getSeaLevel(world) - 7)
                {
                    explodedBlocks.add(block);
                }
            }
        }
        
        //clear original damage list and replace with allowed damage list
        blocks.clear();
        blocks.addAll(explodedBlocks);
    }
	
	//when an item spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemSpawn(ItemSpawnEvent event)
	{
		//if in a creative world, cancel the event (don't drop items on the ground)
		if(GriefPrevention.instance.creativeRulesApply(event.getLocation()))
		{
			event.setCancelled(true);
		}
		
		//if item is on watch list, apply protection
		ArrayList<PendingItemProtection> watchList = GriefPrevention.instance.pendingItemWatchList;
		Item newItem = event.getEntity();
		Long now = null;
		for(int i = 0; i < watchList.size(); i++)
		{
		    PendingItemProtection pendingProtection = watchList.get(i);
		    //ignore and remove any expired pending protections
            if(now == null) now = System.currentTimeMillis();
            if(pendingProtection.expirationTimestamp < now)
            {
                watchList.remove(i--);
                continue;
            }
		    //skip if item stack doesn't match
		    if(pendingProtection.itemStack.getAmount() != newItem.getItemStack().getAmount() ||
		       pendingProtection.itemStack.getType() != newItem.getItemStack().getType())
		    {
		        continue;
		    }
		    
		    //skip if new item location isn't near the expected spawn area 
		    Location spawn = event.getLocation();
		    Location expected = pendingProtection.location;
		    if(!spawn.getWorld().equals(expected.getWorld()) ||
		       spawn.getX() < expected.getX() - 5 ||
		       spawn.getX() > expected.getX() + 5 ||
		       spawn.getZ() < expected.getZ() - 5 ||
		       spawn.getZ() > expected.getZ() + 5 ||
		       spawn.getY() < expected.getY() - 15 ||
		       spawn.getY() > expected.getY() + 3)
		    {
		        continue;
		    }
		    
		    //otherwise, mark item with protection information
		    newItem.setMetadata("GP_ITEMOWNER", new FixedMetadataValue(GriefPrevention.instance, pendingProtection.owner));
		    
		    //and remove pending protection data
		    watchList.remove(i);
		    break;
		}
	}
	
	//when an experience bottle explodes...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onExpBottle(ExpBottleEvent event)
	{
		//if in a creative world, cancel the event (don't drop exp on the ground)
		if(GriefPrevention.instance.creativeRulesApply(event.getEntity().getLocation()))
		{
			event.setExperience(0);
		}
	}
	
	//when a creature spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntitySpawn(CreatureSpawnEvent event)
	{
		//these rules apply only to creative worlds
		if(!GriefPrevention.instance.creativeRulesApply(event.getLocation())) return;
		
		//chicken eggs and breeding could potentially make a mess in the wilderness, once griefers get involved
		SpawnReason reason = event.getSpawnReason();
		if(reason != SpawnReason.SPAWNER_EGG && reason != SpawnReason.BUILD_IRONGOLEM && reason != SpawnReason.BUILD_SNOWMAN && event.getEntityType() != EntityType.ARMOR_STAND)
		{
			event.setCancelled(true);
			return;
		}

		//otherwise, just apply the limit on total entities per claim (and no spawning in the wilderness!)
		Claim claim = this.dataStore.getClaimAt(event.getLocation(), false, null);
		if(claim == null || claim.allowMoreEntities(true) != null)
		{
			event.setCancelled(true);
			return;
		}
	}
	
	//when an entity dies...
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event)
	{
		LivingEntity entity = event.getEntity();
		
		//don't do the rest in worlds where claims are not enabled
        if(!GriefPrevention.instance.claimsEnabledForWorld(entity.getWorld())) return;
		
		//special rule for creative worlds: killed entities don't drop items or experience orbs
		if(GriefPrevention.instance.creativeRulesApply(entity.getLocation()))
		{
			event.setDroppedExp(0);
			event.getDrops().clear();			
		}
		
		//FEATURE: when a player is involved in a siege (attacker or defender role)
		//his death will end the siege
		
		if(!(entity instanceof Player)) return;  //only tracking players
		
		Player player = (Player)entity;
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		
		//if involved in a siege
		if(playerData.siegeData != null)
		{
			//end it, with the dieing player being the loser
			this.dataStore.endSiege(playerData.siegeData, null, player.getName(), event.getDrops());
		}
		
		//FEATURE: lock dropped items to player who dropped them
        
        World world = entity.getWorld();
        
        //decide whether or not to apply this feature to this situation (depends on the world where it happens)
        boolean isPvPWorld = GriefPrevention.instance.pvpRulesApply(world);
        if((isPvPWorld && GriefPrevention.instance.config_lockDeathDropsInPvpWorlds) || 
           (!isPvPWorld && GriefPrevention.instance.config_lockDeathDropsInNonPvpWorlds))
        {
            Claim claim = this.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
            ProtectDeathDropsEvent protectionEvent = new ProtectDeathDropsEvent(claim);
            Bukkit.getPluginManager().callEvent(protectionEvent);
            if(!protectionEvent.isCancelled())
            {
                //remember information about these drops so that they can be marked when they spawn as items
                long expirationTime = System.currentTimeMillis() + 3000;  //now + 3 seconds
                Location deathLocation = player.getLocation();
                UUID playerID = player.getUniqueId();
                List<ItemStack> drops = event.getDrops();
                for(ItemStack stack : drops)
                {
                    GriefPrevention.instance.pendingItemWatchList.add(
                           new PendingItemProtection(deathLocation, playerID, expirationTime, stack));
                }
                
                //allow the player to receive a message about how to unlock any drops
                playerData.dropsAreUnlocked = false;
                playerData.receivedDropUnlockAdvertisement = false;
            }
        }
	}
	
	//when an entity picks up an item
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityPickup(EntityChangeBlockEvent event)
	{
		//FEATURE: endermen don't steal claimed blocks
		
		//if its an enderman
		if(event.getEntity() instanceof Enderman)
		{
			//and the block is claimed
			if(this.dataStore.getClaimAt(event.getBlock().getLocation(), false, null) != null)
			{
				//he doesn't get to steal it
				event.setCancelled(true);
			}
		}
	}
	
	//when a painting is broken
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onHangingBreak(HangingBreakEvent event)
    {
	    //don't track in worlds where claims are not enabled
        if(!GriefPrevention.instance.claimsEnabledForWorld(event.getEntity().getWorld())) return;
	    
	    //FEATURE: claimed paintings are protected from breakage
		
		//explosions don't destroy hangings
	    if(event.getCause() == RemoveCause.EXPLOSION)
	    {
	        event.setCancelled(true);
	        return;
	    }
	    
	    //only allow players to break paintings, not anything else (like water and explosions)
		if(!(event instanceof HangingBreakByEntityEvent))
    	{
        	event.setCancelled(true);
        	return;
    	}
        
        HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent)event;
        
        //who is removing it?
		Entity remover = entityEvent.getRemover();
        
		//again, making sure the breaker is a player
		if(!(remover instanceof Player))
        {
        	event.setCancelled(true);
        	return;
        }
		
		//if the player doesn't have build permission, don't allow the breakage
		Player playerRemover = (Player)entityEvent.getRemover();
        String noBuildReason = GriefPrevention.instance.allowBuild(playerRemover, event.getEntity().getLocation(), Material.AIR);
        if(noBuildReason != null)
        {
        	event.setCancelled(true);
        	GriefPrevention.sendMessage(playerRemover, TextMode.Err, noBuildReason);
        }
    }
	
	//when a painting is placed...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPaintingPlace(HangingPlaceEvent event)
	{
	    //don't track in worlds where claims are not enabled
        if(!GriefPrevention.instance.claimsEnabledForWorld(event.getBlock().getWorld())) return;
	    
	    //FEATURE: similar to above, placing a painting requires build permission in the claim
	
		//if the player doesn't have permission, don't allow the placement
		String noBuildReason = GriefPrevention.instance.allowBuild(event.getPlayer(), event.getEntity().getLocation(), Material.PAINTING);
        if(noBuildReason != null)
        {
        	event.setCancelled(true);
        	GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, noBuildReason);
			return;
        }
		
		//otherwise, apply entity-count limitations for creative worlds
		else if(GriefPrevention.instance.creativeRulesApply(event.getEntity().getLocation()))
		{
			PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getUniqueId());
			Claim claim = this.dataStore.getClaimAt(event.getBlock().getLocation(), false, playerData.lastClaim);
			if(claim == null) return;
			
			String noEntitiesReason = claim.allowMoreEntities(false);
			if(noEntitiesReason != null)
			{
				GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, noEntitiesReason);
				event.setCancelled(true);
				return;
			}
		}
	}
	
	private boolean isMonster(Entity entity)
    {
        if(entity instanceof Monster) return true;
        
        EntityType type = entity.getType();
        if(type == EntityType.GHAST || type == EntityType.MAGMA_CUBE || type == EntityType.SHULKER || type == EntityType.POLAR_BEAR) return true;
        
        if(type == EntityType.RABBIT)
        {
            Rabbit rabbit = (Rabbit)entity;
            if(rabbit.getRabbitType() == Rabbit.Type.THE_KILLER_BUNNY) return true;
        }
        
        return false;
    }
	
	//when an entity is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityDamage (EntityDamageEvent event)
	{
		this.handleEntityDamageEvent(event, true);
	}
	
	//when an entity is set on fire
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityCombustByEntity (EntityCombustByEntityEvent event)
	{
	    //handle it just like we would an entity damge by entity event, except don't send player messages to avoid double messages
	    //in cases like attacking with a flame sword or flame arrow, which would ALSO trigger the direct damage event handler
	    @SuppressWarnings("deprecation")
        EntityDamageByEntityEvent eventWrapper = new EntityDamageByEntityEvent(event.getCombuster(), event.getEntity(), DamageCause.FIRE_TICK, event.getDuration());
	    this.handleEntityDamageEvent(eventWrapper, false);
	    event.setCancelled(eventWrapper.isCancelled());
	}
	
	private void handleEntityDamageEvent(EntityDamageEvent event, boolean sendErrorMessagesToPlayers)
	{
	    //monsters are never protected
        if(isMonster(event.getEntity())) return;
        
        //horse protections can be disabled
        if(event.getEntity() instanceof Horse && !GriefPrevention.instance.config_claims_protectHorses) return;
        
        //protected death loot can't be destroyed, only picked up or despawned due to expiration
        if(event.getEntityType() == EntityType.DROPPED_ITEM)
        {
            if(event.getEntity().hasMetadata("GP_ITEMOWNER"))
            {
                event.setCancelled(true);
            }
        }
        
        //protect pets from environmental damage types which could be easily caused by griefers
        if(event.getEntity() instanceof Tameable && !GriefPrevention.instance.pvpRulesApply(event.getEntity().getWorld()))
        {
            Tameable tameable = (Tameable)event.getEntity();
            if(tameable.isTamed())
            {
                DamageCause cause = event.getCause();
                if( cause != null && (
                    cause == DamageCause.ENTITY_EXPLOSION ||
                    cause == DamageCause.FALLING_BLOCK ||
                    cause == DamageCause.FIRE ||
                    cause == DamageCause.FIRE_TICK ||
                    cause == DamageCause.LAVA ||
                    cause == DamageCause.SUFFOCATION))
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        //the rest is only interested in entities damaging entities (ignoring environmental damage)
        if(!(event instanceof EntityDamageByEntityEvent)) return;
        
        EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
        
        //determine which player is attacking, if any
        Player attacker = null;
        Projectile arrow = null;
        Entity damageSource = subEvent.getDamager();
        
        if(damageSource != null)
        {
            if(damageSource instanceof Player)
            {
                attacker = (Player)damageSource;
            }
            else if(damageSource instanceof Projectile)
            {
                arrow = (Projectile)damageSource;
                if(arrow.getShooter() instanceof Player)
                {
                    attacker = (Player)arrow.getShooter();
                }
            }
            
            //protect players from lingering potion damage when protected from pvp
            if(damageSource.getType() == EntityType.AREA_EFFECT_CLOUD && event.getEntityType() == EntityType.PLAYER && GriefPrevention.instance.pvpRulesApply(event.getEntity().getWorld()))
            {
                Player damaged = (Player)event.getEntity();
                PlayerData damagedData = GriefPrevention.instance.dataStore.getPlayerData(damaged.getUniqueId());
                
                //case 1: recently spawned
                if(GriefPrevention.instance.config_pvp_protectFreshSpawns && damagedData.pvpImmune)
                {
                    event.setCancelled(true);
                    return;
                }
                
                //case 2: in a pvp safe zone
                else
                {
                    Claim damagedClaim = GriefPrevention.instance.dataStore.getClaimAt(damaged.getLocation(), false, damagedData.lastClaim);
                    if(damagedClaim != null)
                    {
                        damagedData.lastClaim = damagedClaim;
                        if(GriefPrevention.instance.claimIsPvPSafeZone(damagedClaim))
                        {
                            PreventPvPEvent pvpEvent = new PreventPvPEvent(damagedClaim);
                            Bukkit.getPluginManager().callEvent(pvpEvent);
                            if(!pvpEvent.isCancelled())
                            {
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }
        }
        
        //if the attacker is a player and defender is a player (pvp combat)
        if(attacker != null && event.getEntity() instanceof Player && GriefPrevention.instance.pvpRulesApply(attacker.getWorld()))
        {
            //FEATURE: prevent pvp in the first minute after spawn, and prevent pvp when one or both players have no inventory
            
            Player defender = (Player)(event.getEntity());
            
            if(attacker != defender)
            {
                PlayerData defenderData = this.dataStore.getPlayerData(((Player)event.getEntity()).getUniqueId());
                PlayerData attackerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                
                //otherwise if protecting spawning players
                if(GriefPrevention.instance.config_pvp_protectFreshSpawns)
                {
                    if(defenderData.pvpImmune)
                    {
                        event.setCancelled(true);
                        if(sendErrorMessagesToPlayers) GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.ThatPlayerPvPImmune);
                        return;
                    }
                    
                    if(attackerData.pvpImmune)
                    {
                        event.setCancelled(true);
                        if(sendErrorMessagesToPlayers) GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                        return;
                    }       
                }
                
                //FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
                if(GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims || GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims)
                {
                    Claim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
    				Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                    if(!attackerData.ignoreClaims)
                    {
    				    if(	attackerClaim != null  && !attackerClaim.isPvpAllowed && //ignore claims mode allows for pvp inside land claims
                            !attackerData.inPvpCombat() &&
                            GriefPrevention.instance.claimIsPvPSafeZone(attackerClaim))
                        {
                            attackerData.lastClaim = attackerClaim;
                            PreventPvPEvent pvpEvent = new PreventPvPEvent(attackerClaim);
                            Bukkit.getPluginManager().callEvent(pvpEvent);
                            if(!pvpEvent.isCancelled())
                            {
                                event.setCancelled(true);
                                if(sendErrorMessagesToPlayers) GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                                return;
                            }
                        }
                        
        				if( defenderClaim != null && !defenderClaim.isPvpAllowed &&
                            !defenderData.inPvpCombat() &&
                            GriefPrevention.instance.claimIsPvPSafeZone(defenderClaim))
                        {
                            defenderData.lastClaim = defenderClaim;
                            PreventPvPEvent pvpEvent = new PreventPvPEvent(defenderClaim);
                            Bukkit.getPluginManager().callEvent(pvpEvent);
                            if(!pvpEvent.isCancelled())
                            {
                                event.setCancelled(true);
                                if(sendErrorMessagesToPlayers) GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                                return;
                            }
                        }
                    }
                }
			}
		}

        //FEATURE: protect claimed animals, boats, minecarts, and items inside item frames
        //NOTE: animals can be lead with wheat, vehicles can be pushed around.
        //so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
        
        //if theft protection is enabled
        if(event instanceof EntityDamageByEntityEvent)
        {
            //don't track in worlds where claims are not enabled
            if(!GriefPrevention.instance.claimsEnabledForWorld(event.getEntity().getWorld())) return;

            //protect players from being attacked by other players' pets when protected from pvp
            if(event.getEntityType() == EntityType.PLAYER)
            {
                Player defender = (Player)event.getEntity();
                
                //if attacker is a pet
                Entity damager = subEvent.getDamager();
                if(damager != null && damager instanceof Tameable)
                {
                    Tameable pet = (Tameable) damager;
                    if(pet.isTamed() && pet.getOwner() != null)
                    {
                        //if defender is NOT in pvp combat and not immune to pvp right now due to recent respawn
                        PlayerData defenderData = GriefPrevention.instance.dataStore.getPlayerData(event.getEntity().getUniqueId());
                        if(!defenderData.pvpImmune && !defenderData.inPvpCombat())
                        {
                            //if defender is not in a protected area
                            Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                            if( defenderClaim != null &&
                                !defenderData.inPvpCombat() &&
                                GriefPrevention.instance.claimIsPvPSafeZone(defenderClaim))
                            {
                                defenderData.lastClaim = defenderClaim;
                                PreventPvPEvent pvpEvent = new PreventPvPEvent(defenderClaim);
                                Bukkit.getPluginManager().callEvent(pvpEvent);
                                
                                //if other plugins aren't making an exception to the rule 
                                if(!pvpEvent.isCancelled())
                                {
                                    event.setCancelled(true);
                                    if(damager instanceof Creature) ((Creature) damager).setTarget(null);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            
            //if the damaged entity is a claimed item frame or armor stand, the damager needs to be a player with build trust in the claim
            if(subEvent.getEntityType() == EntityType.ITEM_FRAME
               || subEvent.getEntityType() == EntityType.ARMOR_STAND
               || subEvent.getEntityType() == EntityType.VILLAGER
               || subEvent.getEntityType() == EntityType.ENDER_CRYSTAL)
            {
                //allow for disabling villager protections in the config
                if(subEvent.getEntityType() == EntityType.VILLAGER && !GriefPrevention.instance.config_claims_protectCreatures) return;
                
                //don't protect polar bears, they may be aggressive
                if(subEvent.getEntityType() == EntityType.POLAR_BEAR) return;
                
                //decide whether it's claimed
                Claim cachedClaim = null;
                PlayerData playerData = null;
                if(attacker != null)
                {
                    playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                    cachedClaim = playerData.lastClaim;
                }
                
                Claim claim = this.dataStore.getClaimAt(event.getEntity().getLocation(), false, cachedClaim);
                
                //if it's claimed
                if(claim != null)
                {
                    //if attacker isn't a player, cancel
                    if(attacker == null)
                    {               
                        event.setCancelled(true);
                        return;
                    }
                    
                    //otherwise player must have container trust in the claim
                    String failureReason = claim.allowBuild(attacker, Material.AIR);
                    if(failureReason != null)
                    {
                        event.setCancelled(true);
                        if(sendErrorMessagesToPlayers) GriefPrevention.sendMessage(attacker, TextMode.Err, failureReason);
                        return;
                    }
                }
            }
            
            //if the entity is an non-monster creature (remember monsters disqualified above), or a vehicle
            if (((subEvent.getEntity() instanceof Creature || subEvent.getEntity() instanceof WaterMob) && GriefPrevention.instance.config_claims_protectCreatures))
            {
                //if entity is tameable and has an owner, apply special rules
                if(subEvent.getEntity() instanceof Tameable)
                {
                    Tameable tameable = (Tameable)subEvent.getEntity();
                    if(tameable.isTamed() && tameable.getOwner() != null)
                    {
                        //limit attacks by players to owners and admins in ignore claims mode
                        if(attacker != null)
                        {
                            UUID ownerID = tameable.getOwner().getUniqueId();
                           
                            //if the player interacting is the owner, always allow
                            if(attacker.getUniqueId().equals(ownerID)) return;
                            
                            //allow for admin override
                            PlayerData attackerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                            if(attackerData.ignoreClaims) return;
                           
                            //otherwise disallow in non-pvp worlds (and also pvp worlds if configured to do so)
                            if(!GriefPrevention.instance.pvpRulesApply(subEvent.getEntity().getLocation().getWorld()) || (GriefPrevention.instance.config_pvp_protectPets && subEvent.getEntityType() != EntityType.WOLF))
                            {
                                OfflinePlayer owner = GriefPrevention.instance.getServer().getOfflinePlayer(ownerID); 
                                String ownerName = owner.getName();
                                if(ownerName == null) ownerName = "someone";
                                String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, ownerName);
                                if(attacker.hasPermission("griefprevention.ignoreclaims"))
                                    message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                                if(sendErrorMessagesToPlayers) GriefPrevention.sendMessage(attacker, TextMode.Err, message);
                                PreventPvPEvent pvpEvent = new PreventPvPEvent(new Claim(subEvent.getEntity().getLocation(), subEvent.getEntity().getLocation(), null, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), null, false, new ArrayList<String>()));
                                Bukkit.getPluginManager().callEvent(pvpEvent);
                                if(!pvpEvent.isCancelled())
                                {
                                    event.setCancelled(true);
                                }
                                return;
                            }
                            //and disallow if attacker is pvp immune
                            else if(attackerData.pvpImmune)
                            {
                                event.setCancelled(true);
                                if(sendErrorMessagesToPlayers) GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                                return;
                            }
                        }
                    }
                }
                
                Claim cachedClaim = null;
                PlayerData playerData = null;
                
                //if not a player or an explosive, allow
                if(attacker == null
                        && damageSource != null
                        && damageSource.getType() != EntityType.CREEPER
                        && damageSource.getType() != EntityType.WITHER
                        && damageSource.getType() != EntityType.ENDER_CRYSTAL
                        && damageSource.getType() != EntityType.AREA_EFFECT_CLOUD
                        && !(damageSource instanceof Projectile)
                        && !(damageSource instanceof Explosive)
                        && !(damageSource instanceof ExplosiveMinecart))
                {
                    return;
                }
                
                if(attacker != null)
                {
                    playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                    cachedClaim = playerData.lastClaim;
                }
                
                Claim claim = this.dataStore.getClaimAt(event.getEntity().getLocation(), false, cachedClaim);
                
                //if it's claimed
                if(claim != null)
                {
                    //if damaged by anything other than a player (exception villagers injured by zombies in admin claims), cancel the event
                    //why exception?  so admins can set up a village which can't be CHANGED by players, but must be "protected" by players.
                    if(attacker == null)
                    {
                        //exception case
                        if(event.getEntity() instanceof Villager && damageSource != null && damageSource instanceof Zombie)
                        {
                            return;
                        }
                        
                        //all other cases
                        else
                        {
                            event.setCancelled(true);
                            if(damageSource != null && damageSource instanceof Projectile)
                            {
                                damageSource.remove();
                            }
                        }                       
                    }
                    
                    //otherwise the player damaging the entity must have permission, unless it's a dog in a pvp world
                    else if(!(event.getEntity().getWorld().getPVP() && event.getEntity().getType() == EntityType.WOLF))
                    {
                        String noContainersReason = claim.allowContainers(attacker);
                        if(noContainersReason != null)
                        {
                            event.setCancelled(true);
                            
                            //kill the arrow to avoid infinite bounce between crowded together animals
                            if(arrow != null) arrow.remove();
                            
                            if(sendErrorMessagesToPlayers)
                            {
                                String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                                if(attacker.hasPermission("griefprevention.ignoreclaims"))
                                    message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                                GriefPrevention.sendMessage(attacker, TextMode.Err, message);
                            }
                            event.setCancelled(true);
                        }
                        
                        //cache claim for later
                        if(playerData != null)
                        {
                            playerData.lastClaim = claim;
                        }                       
                    }
                }
            }
        }
	}
	
	//when an entity is damaged
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageMonitor (EntityDamageEvent event)
    {
        //FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
        //FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated
        
        if(event.getEntity().getType() != EntityType.PLAYER) return;
        
        Player defender = (Player)event.getEntity();
        
        //only interested in entities damaging entities (ignoring environmental damage)
        if(!(event instanceof EntityDamageByEntityEvent)) return;
        
        EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
        
        //if not in a pvp rules world, do nothing
        if(!GriefPrevention.instance.pvpRulesApply(defender.getWorld())) return;
        
        //determine which player is attacking, if any
        Player attacker = null;
        Projectile arrow = null;
        Entity damageSource = subEvent.getDamager();
        
        if(damageSource != null)
        {
            if(damageSource instanceof Player)
            {
                attacker = (Player)damageSource;
            }
            else if(damageSource instanceof Projectile)
            {
                arrow = (Projectile)damageSource;
                if(arrow.getShooter() instanceof Player)
                {
                    attacker = (Player)arrow.getShooter();
                }
            }
        }
        
        //if attacker not a player, do nothing
        if(attacker == null) return;
        
        PlayerData defenderData = this.dataStore.getPlayerData(defender.getUniqueId());
        PlayerData attackerData = this.dataStore.getPlayerData(attacker.getUniqueId());
        
        if(attacker != defender)
        {
        	//warn people that they were placed in pvp combat
            if(!(defenderData.inPvpCombat()) && defender != null)
            {
    			GriefPrevention.sendMessage(defender, TextMode.Warn, Messages.CombatTaggedDefender, attacker.getName(), 
    					((Integer) GriefPrevention.instance.config_pvp_combatTimeoutSeconds).toString());
            }
    		if(!(attackerData.inPvpCombat()) && attacker != null)
    		{
    			GriefPrevention.sendMessage(attacker, TextMode.Warn, Messages.CombatTaggedAttacker, defender.getName(), 
    					((Integer)GriefPrevention.instance.config_pvp_combatTimeoutSeconds).toString());
    		}
        	
            long now = Calendar.getInstance().getTimeInMillis();
            defenderData.lastPvpTimestamp = now;
            defenderData.lastPvpPlayer = attacker.getName();
            attackerData.lastPvpTimestamp = now;
            attackerData.lastPvpPlayer = defender.getName();
            
            CombatStartedEvent startCombat = new CombatStartedEvent(attacker, defender);
            Bukkit.getPluginManager().callEvent(startCombat);
            
        }
    }
	
	//when a vehicle is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onVehicleDamage (VehicleDamageEvent event)
	{
		//all of this is anti theft code
		if(!GriefPrevention.instance.config_claims_preventTheft) return;	
		
		//input validation
		if(event.getVehicle() == null) return;
		
		//don't track in worlds where claims are not enabled
        if(!GriefPrevention.instance.claimsEnabledForWorld(event.getVehicle().getWorld())) return;
		
		//determine which player is attacking, if any
		Player attacker = null;
		Entity damageSource = event.getAttacker();
		EntityType damageSourceType = null;

		//if damage source is null or a creeper, don't allow the damage when the vehicle is in a land claim
		if(damageSource != null)
		{
		    damageSourceType = damageSource.getType();

		    if(damageSource.getType() == EntityType.PLAYER)
    		{
    			attacker = (Player)damageSource;
    		}
    		else if(damageSource instanceof Projectile)
    		{
    		    Projectile arrow = (Projectile)damageSource;
    		    if(arrow.getShooter() instanceof Player)
    			{
    				attacker = (Player)arrow.getShooter();
    			}
    		}
		}
		
		//if not a player and not an explosion, always allow
        if(attacker == null && damageSourceType != EntityType.CREEPER && damageSourceType != EntityType.WITHER && damageSourceType != EntityType.PRIMED_TNT)
        {
            return;
        }
		
		//NOTE: vehicles can be pushed around.
		//so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
		Claim cachedClaim = null;
		PlayerData playerData = null;
		
		if(attacker != null)
		{
			playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
			cachedClaim = playerData.lastClaim;
		}
		
		Claim claim = this.dataStore.getClaimAt(event.getVehicle().getLocation(), false, cachedClaim);
		
		//if it's claimed
		if(claim != null)
		{
			//if damaged by anything other than a player, cancel the event
			if(attacker == null)
			{
				event.setCancelled(true);
			}
			
			//otherwise the player damaging the entity must have permission
			else
			{		
				String noContainersReason = claim.allowContainers(attacker);
				if(noContainersReason != null)
				{
					event.setCancelled(true);
					String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                    if(attacker.hasPermission("griefprevention.ignoreclaims"))
                        message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                    GriefPrevention.sendMessage(attacker, TextMode.Err, message);
                    event.setCancelled(true);
				}
				
				//cache claim for later
				if(playerData != null)
				{
					playerData.lastClaim = claim;
				}						
			}
		}
	}
	
	//when a splash potion effects one or more entities...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPotionSplash (PotionSplashEvent event)
	{
	    ThrownPotion potion = event.getPotion();
	    
	    //ignore potions not thrown by players
	    ProjectileSource projectileSource = potion.getShooter();
        if(projectileSource == null || !(projectileSource instanceof Player)) return;
        Player thrower = (Player)projectileSource;
        
	    Collection<PotionEffect> effects = potion.getEffects();
	    for(PotionEffect effect : effects)
	    {
	        PotionEffectType effectType = effect.getType();

	        //restrict some potions on claimed animals (griefers could use this to kill or steal animals over fences)
	        if(effectType.getName().equals("JUMP") || effectType.getName().equals("POISON"))
	        {
	            for(LivingEntity effected : event.getAffectedEntities())
	            {
	                Claim cachedClaim = null; 
	                if(effected instanceof Animals)
	                {
	                    Claim claim = this.dataStore.getClaimAt(effected.getLocation(), false, cachedClaim);
	                      if(claim != null)
	                      {
	                          cachedClaim = claim;
	                          if(claim.allowContainers(thrower) != null)
	                          {
	                              event.setCancelled(true);
	                              GriefPrevention.sendMessage(thrower, TextMode.Err, Messages.NoDamageClaimedEntity, claim.getOwnerName());
	                              return;
	                          }
	                      }
	                }
	            }
	        }
	        
	        //otherwise, no restrictions for positive effects
            if(positiveEffects.contains(effectType)) continue;
	        
	        for(LivingEntity effected : event.getAffectedEntities())
	        {
	            //always impact the thrower
	            if(effected == thrower) continue;
	            
	            //always impact non players
	            if(!(effected instanceof Player)) continue;
	            
	            //otherwise if in no-pvp zone, stop effect
	            //FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
	            else if(GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims || GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims)
	            {
	                Player effectedPlayer = (Player)effected;
	                PlayerData defenderData = this.dataStore.getPlayerData(effectedPlayer.getUniqueId());
	                PlayerData attackerData = this.dataStore.getPlayerData(thrower.getUniqueId());
	                Claim attackerClaim = this.dataStore.getClaimAt(thrower.getLocation(), false, attackerData.lastClaim);
	                if(attackerClaim != null && GriefPrevention.instance.claimIsPvPSafeZone(attackerClaim))
	                {
	                    attackerData.lastClaim = attackerClaim;
	                    PreventPvPEvent pvpEvent = new PreventPvPEvent(attackerClaim);
                        Bukkit.getPluginManager().callEvent(pvpEvent);
                        if(!pvpEvent.isCancelled())
                        {
    	                    event.setIntensity(effected, 0);
    	                    GriefPrevention.sendMessage(thrower, TextMode.Err, Messages.CantFightWhileImmune);
    	                    continue;
                        }
	                }
	                
	                Claim defenderClaim = this.dataStore.getClaimAt(effectedPlayer.getLocation(), false, defenderData.lastClaim);
	                if(defenderClaim != null && GriefPrevention.instance.claimIsPvPSafeZone(defenderClaim))
	                {
	                    defenderData.lastClaim = defenderClaim;
	                    PreventPvPEvent pvpEvent = new PreventPvPEvent(defenderClaim);
                        Bukkit.getPluginManager().callEvent(pvpEvent);
                        if(!pvpEvent.isCancelled())
                        {
    	                    event.setIntensity(effected, 0);
    	                    GriefPrevention.sendMessage(thrower, TextMode.Err, Messages.PlayerInPvPSafeZone);
    	                    continue;
                        }
	                }
	            }
	        }
	    }
	}
	
	public static final HashSet<PotionEffectType> positiveEffects = new HashSet<PotionEffectType>(Arrays.asList
	(
	    PotionEffectType.ABSORPTION,
	    PotionEffectType.DAMAGE_RESISTANCE,
	    PotionEffectType.FAST_DIGGING,
	    PotionEffectType.FIRE_RESISTANCE,
	    PotionEffectType.HEAL,
	    PotionEffectType.HEALTH_BOOST,
	    PotionEffectType.INCREASE_DAMAGE,
	    PotionEffectType.INVISIBILITY,
	    PotionEffectType.JUMP,
	    PotionEffectType.NIGHT_VISION,
	    PotionEffectType.REGENERATION,
	    PotionEffectType.SATURATION,
	    PotionEffectType.SPEED,
	    PotionEffectType.WATER_BREATHING
	));
}
