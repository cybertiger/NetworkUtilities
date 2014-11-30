package me.olivervscreeper.networkutilities.game;

import gnu.trove.map.hash.THashMap;
import me.olivervscreeper.networkutilities.NetworkUtilities;
import me.olivervscreeper.networkutilities.game.events.GameSwitchStateEvent;
import me.olivervscreeper.networkutilities.game.events.PlayerDeathInArenaEvent;
import me.olivervscreeper.networkutilities.game.events.PlayerJoinGameEvent;
import me.olivervscreeper.networkutilities.game.events.PlayerLeaveGameEvent;
import me.olivervscreeper.networkutilities.game.players.GamePlayer;
import me.olivervscreeper.networkutilities.game.states.GameState;
import me.olivervscreeper.networkutilities.game.states.IdleGameState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created on 29/11/2014.
 *
 * @author OliverVsCreeper
 */
public abstract class NetworkGame implements Listener{

    //TODO: Add basic system for Arenas

    GameState currentState = new IdleGameState(this); //State of the Game
    List<GameState> registeredStates = new ArrayList<GameState>();
    Iterator stateIterator;

    THashMap<String, GamePlayer> players = new THashMap<String, GamePlayer>();
    THashMap<String, GamePlayer> spectators = new THashMap<String, GamePlayer>();

    public abstract String getName();
    public abstract List<GameState> getAllStates();
    public abstract Location getLobbyLocation();

    public NetworkGame(){
        Bukkit.getPluginManager().registerEvents(this, NetworkUtilities.plugin);
    }

    public GameState getState(){return currentState;}

    public Boolean setState(GameState state){
        //Throw the linked event, and end the action if the event becomes cancelled
        GameSwitchStateEvent event = new GameSwitchStateEvent(this, state);
        Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()) return false;

        if(!currentState.onStateEnd()) return false;
        if(!state.onStateBegin()) return false;
        currentState = state;
        return true;
    }

    public void tick(){
        currentState.tick();
        currentState.incrementRuntime();
    }

    public Boolean nextState(){
        if(stateIterator == null | !stateIterator.hasNext()){
            stateIterator = registeredStates.iterator();
        }
        return setState((GameState) stateIterator.next()); //Set state to the next possible state
    }

    public void registerState(GameState state){
        registeredStates.add(state);
    }

    public Boolean addPlayer(Player player){
        //Throw the linked event, and end the action if the event becomes cancelled
        PlayerJoinGameEvent event = new PlayerJoinGameEvent(this, player, currentState);
        Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()) return false;

        players.put(player.getName(), new GamePlayer(player.getName(), this));
        players.get(player.getName()).saveData();
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(getLobbyLocation());
        return true;
    }

    public Boolean removePlayer(Player player){
        //Throw the linked event, and end the action if the event becomes cancelled
        PlayerLeaveGameEvent event = new PlayerLeaveGameEvent(this, player, currentState);
        Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()) return false;

        players.get(player.getName()).resetData();
        players.remove(player.getName());
        return true;
    }

    public void addSpectator(Player player){
        spectators.put(player.getName(), new GamePlayer(player.getName(), this));
        players.get(player.getName()).saveData();
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(getLobbyLocation());
    }

    public void removeSpectator(Player player){
        spectators.get(player.getName()).resetData();
        spectators.remove(player.getName());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event){
        if(!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if(!(players.keySet().contains(player.getName()))) return;
        if(!(player.getHealth() - event.getDamage() <= 0)) return;
        event.setCancelled(true);
        player.setHealth(20D);

        //Throw the linked event - cannot be cancelled
        PlayerDeathInArenaEvent newEvent = new PlayerDeathInArenaEvent(this, player);
        Bukkit.getPluginManager().callEvent(newEvent);
    }

}