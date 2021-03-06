package net.lobby_simulator_companion.loop.domain;

import net.lobby_simulator_companion.loop.domain.stats.Stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Container class for the app stored data.
 *
 * @author NickyRamone
 */
public class LoopData {

    private final int version = 3;
    private final List<Player> players = new ArrayList<>();
    private final Stats stats = new Stats();


    public int getVersion() {
        return version;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void addPlayers(Collection<Player> players) {
        this.players.addAll(players);
    }

    public Stats getStats() {
        return stats;
    }

}
