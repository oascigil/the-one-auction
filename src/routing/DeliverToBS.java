package routing;

import core.Settings;

/** 
* Router that will deliver messages to the closest connected Stationary node, if there the To address is empty.
* Otherwise, route message to the intended destination.
*/
public class DeliverToBS extends ActiveRouter {

    public DeliverToBS (Settings s) {
        super(s);
    }

    public DeliverToBS(DeliverToBS r) {
        super(r);
    }

    @Override
    public void update() {
        super.update();
    }
	
    @Override
	public DeliverToBS replicate() {
		return new DeliverToBS(this);
	}
}

