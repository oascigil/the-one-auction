package routing;

import core.Settings;

/** 
* Router that will deliver messages to the current connected Stationary node (if there is any).
*/
public class DeliverToBS extends ActiveRouter {

    public DeliverToBS (Settings s) {
        super(s);
    }

    @Override
    public void update() {
        super.update();
    }
}

