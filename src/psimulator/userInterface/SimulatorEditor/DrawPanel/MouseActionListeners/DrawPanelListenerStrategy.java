package psimulator.userInterface.SimulatorEditor.DrawPanel.MouseActionListeners;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.undo.UndoManager;
import psimulator.dataLayer.DataLayerFacade;
import psimulator.userInterface.MainWindowInnerInterface;
import psimulator.userInterface.SimulatorEditor.DrawPanel.Components.AbstractComponent;
import psimulator.userInterface.SimulatorEditor.DrawPanel.Components.AbstractHwComponent;
import psimulator.userInterface.SimulatorEditor.DrawPanel.Components.BundleOfCables;
import psimulator.userInterface.SimulatorEditor.DrawPanel.DrawPanelInnerInterface;
import psimulator.userInterface.SimulatorEditor.DrawPanel.Enums.ZoomType;
import psimulator.userInterface.SimulatorEditor.DrawPanel.ZoomManager;
import psimulator.userInterface.SimulatorEditor.Tools.AbstractTool;

/**
 *
 * @author Martin
 */
public abstract class DrawPanelListenerStrategy extends MouseInputAdapter implements MouseWheelListener {

    protected DrawPanelInnerInterface drawPanel;
    protected MainWindowInnerInterface mainWindow;
    protected UndoManager undoManager;
    protected ZoomManager zoomManager;
    protected DataLayerFacade dataLayer;
    
    /**
     * list for all marked components
     */
    //protected List<Markable> markedComponents = new ArrayList<Markable>();

    public DrawPanelListenerStrategy(DrawPanelInnerInterface drawPanel, UndoManager undoManager, ZoomManager zoomManager, 
            MainWindowInnerInterface mainWindow, DataLayerFacade dataLayer) {
        super();

        this.drawPanel = drawPanel;
        this.undoManager = undoManager;
        this.mainWindow = mainWindow;
        this.zoomManager = zoomManager;
        this.dataLayer = dataLayer;
    }

    public abstract void initialize();
    
    public abstract void deInitialize();

    public abstract void setTool(AbstractTool tool);

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            //System.out.println("Point:"+e.getPoint());
            // scroll down
            if (e.getWheelRotation() == 1) {
                zoomManager.zoomIn(e.getPoint(), ZoomType.MOUSE);
                //scroll up    
            } else if (e.getWheelRotation() == -1) {
                zoomManager.zoomOut(e.getPoint(), ZoomType.MOUSE);
            }
        }
    }

    @Override
    public final void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            mouseClickedLeft(e);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            mouseClickedRight(e);
        }
    }

    @Override
    public final void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            mousePressedLeft(e);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            mousePressedRight(e);
        }
    }

    @Override
    public final void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            mouseReleasedLeft(e);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            mouseReleasedRight(e);
        }
    }

    @Override
    public final void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            mouseDraggedLeft(e);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            mouseDraggedRight(e);
        }
    }
    
    public void mousePressedRight(MouseEvent e) {
        drawPanel.doSetDefaultToolInEditorToolBar();
    }

    public void mouseClickedLeft(MouseEvent e) {
    }

    public void mouseClickedRight(MouseEvent e) {
    }

    public void mousePressedLeft(MouseEvent e) {
    }

    public void mouseReleasedLeft(MouseEvent e) {
    }

    public void mouseReleasedRight(MouseEvent e) {
    }

    public void mouseDraggedLeft(MouseEvent e) {
    }

    public void mouseDraggedRight(MouseEvent e) {
    }

    /**
     * Return component at point
     * @param point
     * @return component clicked
     */
    protected AbstractComponent getClickedItem(Point point) {
        // search HwComponents
        AbstractComponent clickedComponent = getClickedAbstractHwComponent(point);

        if (clickedComponent != null) {
            return clickedComponent;
        }

        // create small rectangle arround clicked point
        
        // search cables
        for (BundleOfCables boc : drawPanel.getGraphOuterInterface().getBundlesOfCables()) {
            clickedComponent = boc.getIntersectingCable(point);
            if(clickedComponent != null){
               return clickedComponent; 
            }
        }

        return clickedComponent;
    }
    
    /**
     * Get clicked AbstractHWComponent at point
     * @param point
     * @return 
     */
    protected AbstractHwComponent getClickedAbstractHwComponent(Point point) {
        AbstractHwComponent clickedComponent = null;

        // search HwComponents
        for (AbstractHwComponent c : drawPanel.getGraphOuterInterface().getHwComponents()) {
            if (c.intersects(point)) {
                clickedComponent = c;
                break;
            }
        }

        return clickedComponent;
    }
    
}
