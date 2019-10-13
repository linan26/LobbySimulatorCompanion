package net.lobby_simulator_companion.loop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.DbdLogMonitorEvent.EventType;

/**
 * DBD log parser daemon.
 * Parses logs to extract information about the Steam users that we are connecting to.
 * <p>
 * Note: This approach is quite hacky and not a great solution to the problem, but it does its job fairly easily.
 * A better way would be to obtain the information from the UDP packets, but that's probably not feasible due to
 * encrypted traffic.
 * <p>
 * TODO: see if we can use something better than {@link Observer}.
 *
 * @author NickyRamone
 */
public class DbdLogMonitor extends Observable implements Runnable {

    private static final int LOG_POLLING_PERIOD_MS = 1000;

    private static final Logger logger = LoggerFactory.getLogger(DbdLogMonitor.class);

    private static final Path USER_APPDATA_PATH = Paths.get(System.getenv("APPDATA")).getParent();
    private static final String LOG_PATH = "Local/DeadByDaylight/Saved/Logs/DeadByDaylight.log";
    private static final File LOG_FILE = USER_APPDATA_PATH.resolve(LOG_PATH).toFile();

    private static final String REGEX__LOBBY_KILLER = "MatchMembersA=\\[\"([0-9a-f\\-]+)\"\\]";
    private static final Pattern PATTERN__LOBBY_KILLER = Pattern.compile(REGEX__LOBBY_KILLER);

    private static final String REGEX__LOBBY_ADD_PLAYER = "AddSessionPlayer Session:GameSession PlayerId:([0-9a-f\\-]+)\\|([0-9]+)";
    private static final Pattern PATTERN__LOBBY_ADD_PLAYER = Pattern.compile(REGEX__LOBBY_ADD_PLAYER);

    private static final String REGEX__KILLER_OUTFIT = "LogCustomization: --> ([a-zA-Z0-9]+)_[a-zA-Z0-9]+";
    private static final Pattern PATTERN__KILLER_OUTFIT = Pattern.compile(REGEX__KILLER_OUTFIT);

    private static final String[][] KILLER_NAME_TO_OUTFIT_CODES_MAPPING = new String[][]{
            {"Cannibal", "CA"},
            {"Clown", "GK", "Clown"},
            {"Demogorgon", "QK"},
            {"Doctor", "DO", "DOW04", "Killer07"},
            {"Ghostface", "OK"},
            {"Hag", "HA", "WI", "Witch"},
            {"Hillbilly", "HB", "TC", "Hillbilly"},
            {"Huntress", "BE"},
            {"Legion", "KK", "Legion"},
            {"Nightmare", "SD"},
            {"Nurse", "TN", "Nurse", "NR"},
            {"Pig", "FK"},
            {"Plague", "ML", "MK", "Plague"},
            {"Shape", "MM"},
            {"Spirit", "HK", "Spirit"},
            {"Trapper", "TR", "TRW03", "TRW04", "Chuckles", "S01", "Trapper"},
            {"Wraith", "TW", "WR", "Wraith"}
    };

    private static final Map<String, String> OUTFIT_CODE_TO_KILLER_NAME_MAPPING = new HashMap<>();

    static {
        for (String[] e : KILLER_NAME_TO_OUTFIT_CODES_MAPPING) {
            String killerName = e[0];

            for (int i = 1, n = e.length; i < n; i++) {
                String outfitCode = e[i];
                OUTFIT_CODE_TO_KILLER_NAME_MAPPING.put(outfitCode, killerName);
            }

        }
    }


    public static final class DbdLogMonitorEvent {
        public enum EventType {KILLER_ID, KILLER_CHARACTER}

        public final EventType type;
        public final Object argument;

        public DbdLogMonitorEvent(EventType type, Object argument) {
            this.type = type;
            this.argument = argument;
        }
    }


    private BufferedReader reader;
    private long logSize;
    private Map<String, String> dbdPlayerIdToSteamId = new HashMap<>();
    private String killerDbdId;


    public DbdLogMonitor() throws IOException {
        initReader();
    }


    private void initReader() throws IOException {
        if (reader != null) {
            reader.close();
        }
        reader = new BufferedReader(new FileReader(LOG_FILE));

        // consume all entries in the log file, since they are old and cannot be related to any active connection.
        while (reader.readLine() != null) ;
    }


    @Override
    public void run() {
        String line;

        while (true) {
            try {
                long currentLogSize = LOG_FILE.length();

                if (currentLogSize < logSize) {
                    // the log file has been recreated (probably due to DBD being restarted),
                    // so we need to re-instantiate the reader
                    initReader();
                }
                logSize = currentLogSize;
                line = reader.readLine();

                if (line != null) {
                    processLine(line);
                } else {
                    // for now, there are no more entries in the file
                    Thread.sleep(LOG_POLLING_PERIOD_MS);
                }
            } catch (IOException e) {
                logger.error("Encountered error while processing log file.", e);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void processLine(String line) {
        Matcher matcher;

        matcher = PATTERN__KILLER_OUTFIT.matcher(line);
        if (matcher.find()) {
            String outfitCode = matcher.group(1);
            String killerName = OUTFIT_CODE_TO_KILLER_NAME_MAPPING.get(outfitCode);

            if (killerName != null) {
                setChanged();
                DbdLogMonitorEvent event = new DbdLogMonitorEvent(EventType.KILLER_CHARACTER, killerName);
                notifyObservers(event);
            }
            return;
        }

        matcher = PATTERN__LOBBY_KILLER.matcher(line);
        if (matcher.find()) {
            killerDbdId = matcher.group(1);
            logger.debug("Detected killer player dbd-id: {}", killerDbdId);
            String steamUserId = dbdPlayerIdToSteamId.get(killerDbdId);

            if (steamUserId != null) {
                setChanged();
                PlayerIdWrapper id = new PlayerIdWrapper(steamUserId, killerDbdId);
                DbdLogMonitorEvent event = new DbdLogMonitorEvent(EventType.KILLER_ID, id);
                killerDbdId = null;
                dbdPlayerIdToSteamId.clear();
                notifyObservers(event);
            } else {
                logger.debug("Could not find a steam id for this killer player dbd-id.");
            }
            return;
        }

        matcher = PATTERN__LOBBY_ADD_PLAYER.matcher(line);
        if (matcher.find()) {
            String dbdPlayerId = matcher.group(1);
            String steamUserId = matcher.group(2);
            logger.debug("Detected user connecting to lobby. dbd-id: {}; steam-id: {}", dbdPlayerId, steamUserId);
            dbdPlayerIdToSteamId.put(dbdPlayerId, steamUserId);

            if (dbdPlayerId.equals(killerDbdId)) {
                setChanged();
                PlayerIdWrapper id = new PlayerIdWrapper(steamUserId, killerDbdId);
                DbdLogMonitorEvent event = new DbdLogMonitorEvent(EventType.KILLER_ID, id);
                killerDbdId = null;
                dbdPlayerIdToSteamId.clear();
                notifyObservers(event);
            }
        }
    }

}
