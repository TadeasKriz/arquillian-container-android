package org.jboss.arquillian.android.spi.event;

/**
 * Event representing that an Android virtual device is about to be deleted.
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class AndroidVirtualDeviceDeleted extends AndroidVirtualDeviceEvent {

	public AndroidVirtualDeviceDeleted(String name) {
		super(name);
	}

}
