package controller;

import model.util.SocketMgr;
import view.Simulator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RealRunCheckBoxListener implements ActionListener {

    private Simulator mView;

    public RealRunCheckBoxListener(Simulator view) {
        mView = view;
        mView.addRealRunCheckBoxListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (mView.getIsRealRun()) {
            if (!SocketMgr.getInstance().isConnected()) {
                boolean success = SocketMgr.getInstance().openConnection();

                if (!success) {
                    mView.checkRealRun(false);
                    return;
                }
            }

            mView.disableLoadMapButton();
            mView.disableExplorationButton();
            mView.disableFastestPathButton();
            mView.enableRealRunButton();
        } else {
            mView.enableLoadMapButton();
            mView.enableExplorationButton();
            mView.enableFastestPathButton();
            mView.disableRealRunButton();
        }
    }
}
