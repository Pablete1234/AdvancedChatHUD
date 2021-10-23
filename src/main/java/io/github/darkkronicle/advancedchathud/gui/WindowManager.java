package io.github.darkkronicle.advancedchathud.gui;

import fi.dy.masa.malilib.interfaces.IRenderer;
import io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen;
import io.github.darkkronicle.advancedchatcore.chat.ChatMessage;
import io.github.darkkronicle.advancedchathud.HudChatMessage;
import io.github.darkkronicle.advancedchathud.tabs.AbstractChatTab;
import java.util.LinkedList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;

@Environment(EnvType.CLIENT)
public class WindowManager implements IRenderer {

    private static final WindowManager INSTANCE = new WindowManager();
    private final MinecraftClient client;
    private LinkedList<ChatWindow> windows = new LinkedList<>();
    private int dragX = 0;
    private int dragY = 0;
    private ChatWindow drag = null;
    private boolean resize = false;

    public static WindowManager getInstance() {
        return INSTANCE;
    }

    private WindowManager() {
        client = MinecraftClient.getInstance();
    }

    @Override
    public void onRenderGameOverlayPost(MatrixStack matrixStack) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            windows
                .get(i)
                .render(
                    matrixStack,
                    client.inGameHud.getTicks(),
                    isChatFocused()
                );
        }
    }

    public void resetScroll() {
        for (ChatWindow w : windows) {
            w.resetScroll();
        }
    }

    public void scroll(double amount) {
        for (ChatWindow w : windows) {
            if (w.isSelected()) {
                w.scroll(amount);
            }
        }
    }

    public void scroll(double amount, double mouseX, double mouseY) {
        for (ChatWindow w : windows) {
            if (w.isSelected() || w.isMouseOver(mouseX, mouseY)) {
                w.scroll(amount);
            }
        }
    }

    public Style getText(double mouseX, double mouseY) {
        for (ChatWindow w : windows) {
            if (w.isMouseOver(mouseX, mouseY)) {
                return w.getText(mouseX, mouseY);
            }
        }
        return null;
    }

    public boolean isChatFocused() {
        return this.client.currentScreen instanceof AdvancedChatScreen;
    }

    public ChatWindow getSelected() {
        for (ChatWindow w : windows) {
            if (w.isSelected()) {
                return w;
            }
        }
        return null;
    }

    public void setSelected(ChatWindow window) {
        for (ChatWindow w : windows) {
            w.setSelected(window.equals(w));
        }
        windows.remove(window);
        windows.addFirst(window);
    }

    public boolean mouseClicked(
        Screen screen,
        double mouseX,
        double mouseY,
        int button
    ) {
        ChatWindow over = null;
        for (ChatWindow w : windows) {
            if (w.isMouseOver(mouseX, mouseY)) {
                over = w;
                break;
            }
        }
        if (over == null) {
            return false;
        }
        if (button == 0) {
            setSelected(over);
            if (over.isMouseOverDragBar(mouseX, mouseY)) {
                drag = over;
                dragX = (int) mouseX - over.getX();
                dragY = (int) mouseY - over.getY();
                resize = false;
            } else if (over.isMouseOverResize(mouseX, mouseY)) {
                drag = over;
                dragX = (int) mouseX - over.getWidth();
                dragY = (int) mouseY + over.getHeight();
                resize = true;
            }
            Style style = over.getText(mouseX, mouseY);
            if (style != null && screen.handleTextClick(style)) {
                return true;
            }
            if (over.onMouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return true;
    }

    public boolean mouseDragged(
        double mouseX,
        double mouseY,
        int button,
        double deltaX,
        double deltaY
    ) {
        if (drag != null && !resize) {
            int x = Math.max((int) mouseX - dragX, 0);
            int y = Math.max((int) mouseY - dragY, drag.getActualHeight());
            x =
                Math.min(
                    x,
                    client.getWindow().getScaledWidth() - drag.getWidth()
                );
            y = Math.min(y, client.getWindow().getScaledHeight());
            drag.setPosition(x, y);
            return true;
        } else if (drag != null) {
            int width = Math.max((int) mouseX - dragX, 80);
            int height = Math.max(dragY - (int) mouseY, 40);
            drag.setDimensions(width, height);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(
        double mouseX,
        double mouseY,
        int mouseButton
    ) {
        if (drag != null) {
            drag = null;
            return true;
        }
        return false;
    }

    public void onTabButton(AbstractChatTab tab) {
        for (ChatWindow w : windows) {
            if (w.isSelected()) {
                w.setTab(tab);
            }
        }
    }

    public void onTabAddButton(AbstractChatTab tab) {
        ChatWindow window = new ChatWindow(tab);
        ChatWindow sel = getSelected();
        if (sel == null) {
            sel = window;
        }
        window.setPosition(sel.getX() + 15, sel.getY() + 15);
        windows.add(window);
        setSelected(window);
    }

    public void deleteWindow(ChatWindow chatWindow) {
        windows.remove(chatWindow);
        if (!windows.isEmpty()) {
            for (ChatWindow w : windows) {
                w.setSelected(false);
            }
            windows.get(0).setSelected(true);
        }
    }

    public void onStackedMessage(HudChatMessage message) {
        for (ChatWindow w : windows) {
            w.stackMessage(message);
        }
    }

    public void onNewMessage(HudChatMessage message) {
        for (ChatWindow w : windows) {
            w.addMessage(message);
        }
    }

    public void clear() {
        for (ChatWindow w : windows) {
            w.clearLines();
        }
    }
}