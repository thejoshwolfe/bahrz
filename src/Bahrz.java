import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Bahrz
{
    private static final int PIXELS = 16;
    private static TrayIcon trayIcon;
    private static BufferedImage imageBuffer;
    private static Process process;

    public static void main(String[] args) throws Exception
    {
        process = new ProcessBuilder("typeperf", "\\Processor(*)\\% Processor Time").redirectErrorStream(true).start();
        {
            imageBuffer = new BufferedImage(PIXELS, PIXELS, BufferedImage.TYPE_INT_RGB);
            trayIcon = new TrayIcon(imageBuffer);
            draw(Collections.<Double>emptyList());

            PopupMenu popup = new PopupMenu();
            MenuItem menuItem = new MenuItem("Exit");
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    SystemTray.getSystemTray().remove(trayIcon);
                    process.destroy();
                    System.exit(0);
                }
            });
            popup.add(menuItem);
            trayIcon.setPopupMenu(popup);
            SystemTray.getSystemTray().add(trayIcon);
        }
        {
            Scanner scanner = new Scanner(process.getInputStream());
            // ignore first 2 lines
            scanner.nextLine();
            scanner.nextLine();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                List<String> parts = Arrays.asList(line.split(","));
                // first column is time. last column is average.
                parts = parts.subList(1, parts.size() - 1);
                ArrayList<Double> usages = new ArrayList<>();
                for (String part : parts)
                    usages.add(Double.parseDouble(part.replace("\"", "")) / 100.0);
                draw(usages);
            }
        }
    }

    private static void draw(List<Double> usages)
    {
        Collections.sort(usages, Collections.reverseOrder());

        int coreCount = usages.size();
        int pixelsPerCore = PIXELS;
        while (pixelsPerCore * coreCount > PIXELS)
            pixelsPerCore >>= 1;
        int startOffsetX = (PIXELS - pixelsPerCore * coreCount) / 2;

        Graphics2D graphics = imageBuffer.createGraphics();
        graphics.setBackground(Color.DARK_GRAY);
        graphics.clearRect(0, 0, PIXELS, PIXELS);
        Color[] colors = { new Color(0, 255, 0), new Color(0, 200, 0) };
        for (int i = 0; i < coreCount; i++) {
            graphics.setColor(colors[i % 2]);
            int height = (int)(usages.get(i) * PIXELS);
            graphics.fillRect(startOffsetX + i * pixelsPerCore, PIXELS - height, pixelsPerCore, height);
        }
        graphics.dispose();

        trayIcon.setImage(imageBuffer);
    }
}
