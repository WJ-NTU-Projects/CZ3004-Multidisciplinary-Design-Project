package controller;

import model.util.SocketMgr;
import view.Simulator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Button listener
 */
public class RealRunCheckBoxListener implements ActionListener {

    private Simulator mView;

    public RealRunCheckBoxListener(Simulator view) {
        mView = view;
        mView.addRealRunCheckBoxListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (mView.getIsRealRun()) {
            mView.disableLoadMapButton();
            mView.disableExplorationButton();
            mView.disableFastestPathButton();
            mView.enableRealRunButton();
            if (!SocketMgr.getInstance().isConnected())
                SocketMgr.getInstance().openConnection();
        } else {
            mView.enableLoadMapButton();
            mView.enableExplorationButton();
            mView.enableFastestPathButton();
            mView.disableRealRunButton();
        }
    }
}
