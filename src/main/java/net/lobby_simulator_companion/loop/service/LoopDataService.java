package net.lobby_simulator_companion.loop.service;

import net.lobby_simulator_companion.loop.domain.LoopData;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.domain.Stats;
import net.lobby_simulator_companion.loop.repository.LoopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing data related to players and servers.
 *
 * @author NickyRamone
 */
public class LoopDataService {

    private static final Logger logger = LoggerFactory.getLogger(LoopDataService.class);
    private static final long SAVE_PERIOD_MS = 5000;

    private final LoopRepository repository;

    private LoopData loopData;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private boolean dirty;


    public LoopDataService(LoopRepository loopRepository) throws IOException {
        repository = loopRepository;
        try {
            loopData = repository.load();
        } catch (FileNotFoundException e) {
            loopData = new LoopData();
            repository.save(loopData);
        }

        for (Player player : loopData.getPlayers()) {
            if (player.getUID() != null && !player.getUID().isEmpty()) {
                player.setSteamId64(player.getUID());
                player.setUID(null);
                dirty = true;
            }
            players.put(player.getSteamId64(), player);
        }

        // schedule thread for saving dirty data
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                save();
            }
        }, SAVE_PERIOD_MS, SAVE_PERIOD_MS);
    }

    public Stats getStats() {
        return loopData.getStats();
    }

    public Player getPlayerBySteamId(String steamId) {
        return players.get(steamId);
    }

    public void addPlayer(String steamId, Player player) {
        players.put(steamId, player);
        dirty = true;
    }

    public void notifyChange() {
        dirty = true;
    }

    public synchronized void save() {
        if (!dirty) {
            return;
        }

        loopData.getPlayers().clear();
        loopData.addPlayers(players.values().stream().collect(Collectors.toList()));
        try {
            repository.save(loopData);
            dirty = false;
            logger.debug("Saved Loop data.");
        } catch (IOException e) {
            logger.error("Failed to save data.", e);
        }
    }


}
