package net.nickyramone.deadbydaylight.loop.ui;

import net.nickyramone.deadbydaylight.loop.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * A factory for obtaining singleton instances of different resources.
 *
 * @author NickyRamone
 */
public final class ResourceFactory {

    private static final Logger logger = LoggerFactory.getLogger(ResourceFactory.class);

    private static final String STEAM_ICON_PATH = "/steam_icon.png";
    private static final String THUMBS_DOWN_ICON_PATH = "/thumbs-down_icon.png";
    private static final String THUMBS_UP_ICON_PATH = "/thumbs-up_icon.png";

    private static Font roboto;
    private static ImageIcon thumbsUpIcon;
    private static ImageIcon thumbsDownIcon;
    private static ImageIcon steamIcon;


    private ResourceFactory() {
    }


    public static synchronized Font getRobotoFont() {
        if (roboto == null) {
            InputStream is = FileUtil.localResource("Roboto-Medium.ttf");
            try {
                roboto = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(15f);
            } catch (FontFormatException e) {
                logger.error("Font file is corrupt.");
            } catch (IOException e) {
                logger.error("Failed to load Roboto font.", e);
            }
        }
        return roboto;
    }

    public static synchronized ImageIcon getThumbsUpIcon() {
        if (thumbsUpIcon == null) {
            java.net.URL imgURL = ResourceFactory.class.getResource(THUMBS_UP_ICON_PATH);

            if (imgURL != null) {
                thumbsUpIcon = new ImageIcon(imgURL, "thumbs up");
            } else {
                logger.error("Failed to load thumbs-up icon.");
            }
        }

        return thumbsUpIcon;
    }


    public static synchronized ImageIcon getThumbsDownIcon() {
        if (thumbsDownIcon == null) {
            java.net.URL imgURL = ResourceFactory.class.getResource(THUMBS_DOWN_ICON_PATH);

            if (imgURL != null) {
                thumbsDownIcon = new ImageIcon(imgURL, "thumbs down");
            } else {
                logger.error("Failed to load thumbs-down icon.");
            }
        }

        return thumbsDownIcon;
    }


    public static synchronized ImageIcon getSteamIcon() {
        if (steamIcon == null) {
            java.net.URL imgURL = ResourceFactory.class.getResource(STEAM_ICON_PATH);

            if (imgURL != null) {
                steamIcon = new ImageIcon(imgURL, "steam");
            } else {
                logger.error("Failed to load Steam icon.");
            }
        }

        return steamIcon;
    }


}