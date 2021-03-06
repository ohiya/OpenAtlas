/**
 * OpenAtlasForAndroid Project
 * The MIT License (MIT) Copyright (OpenAtlasForAndroid) 2015 Bunny Blue,achellies
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author BunnyBlue
 **/
package com.openatlas.framework;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

/**
 * ServiceReference  implementation
 ***/
final class ServiceReferenceImpl implements ServiceReference {
    private static final HashSet<String> forbidden;
    private static long nextServiceID;
    Bundle bundle;
    private HashMap<Bundle, Object> cachedServices;
    private final boolean isServiceFactory;
    final Dictionary<String, Object> properties;
    ServiceRegistration registration;
    private Object service;
    final Map<Bundle, Integer> useCounters;

    private final class ServiceRegistrationImpl implements ServiceRegistration {
        private ServiceRegistrationImpl() {
        }

        @Override
        public ServiceReference getReference() {
            if (ServiceReferenceImpl.this.service != null) {
                return ServiceReferenceImpl.this;
            }
            throw new IllegalStateException(
                    "Service has already been uninstalled");
        }

        @Override
        public void setProperties(Dictionary<String, ?> dictionary) {
            if (ServiceReferenceImpl.this.service == null) {
                throw new IllegalStateException(
                        "Service has already been uninstalled");
            }
            HashMap hashMap = new HashMap(
                    ServiceReferenceImpl.this.properties.size());
            Enumeration keys = ServiceReferenceImpl.this.properties.keys();
            while (keys.hasMoreElements()) {
                String serviceKey = (String) keys.nextElement();
                String toLowerCase = serviceKey.toLowerCase(Locale.US);
                if (hashMap.containsKey(toLowerCase)) {
                    throw new IllegalArgumentException(
                            "Properties contain the same key in different case variants");
                }
                hashMap.put(toLowerCase, serviceKey);
            }
            keys = dictionary.keys();
            while (keys.hasMoreElements()) {
                String serviceKey = (String) keys.nextElement();
                Object service = dictionary.get(serviceKey);
                String serviceKeyLowerCase = serviceKey.toLowerCase(Locale.US);
                if (!ServiceReferenceImpl.forbidden.contains(serviceKeyLowerCase)) {
                    Object key = hashMap.get(serviceKeyLowerCase);
                    if (key != null) {
                        if (key.equals(serviceKey)) {
                            ServiceReferenceImpl.this.properties.remove(key);
                        } else {
                            throw new IllegalArgumentException(
                                    "Properties already exists in Component different case variant");
                        }
                    }
                    ServiceReferenceImpl.this.properties.put(serviceKey, service);
                }
            }
            Framework.notifyServiceListeners(BundleEvent.STARTED, ServiceReferenceImpl.this);
        }

        @Override
        public void unregister() {
            if (ServiceReferenceImpl.this.service == null) {
                throw new IllegalStateException(
                        "Service has already been uninstalled");
            }
            Framework.unregisterService(ServiceReferenceImpl.this);
            ServiceReferenceImpl.this.service = null;
        }
    }

    static {
        nextServiceID = 0;
        forbidden = new HashSet();
        forbidden.add(Constants.SERVICE_ID.toLowerCase(Locale.US));
        forbidden.add(Constants.OBJECTCLASS.toLowerCase(Locale.US));
    }

    ServiceReferenceImpl(Bundle bundle, Object service,
                         Dictionary<String, ?> properties, String[] clazzes) {
        this.useCounters = new HashMap<Bundle, Integer>(0);
        this.cachedServices = null;
        if (service instanceof ServiceFactory) {
            this.isServiceFactory = true;
        } else {
            this.isServiceFactory = false;
            checkService(service, clazzes);
        }
        this.bundle = bundle;
        this.service = service;
        this.properties = properties == null ? new Hashtable() : new Hashtable(
                properties.size());
        if (properties != null) {
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                this.properties.put(key, properties.get(key));
            }
        }
        this.properties.put(Constants.OBJECTCLASS, clazzes);
        Dictionary<String, Object> dictionary = this.properties;
        String serviceId = Constants.SERVICE_ID;
        long j = nextServiceID + 1;
        nextServiceID = j;
        dictionary.put(serviceId, Long.valueOf(j));
        Integer num = properties == null ? null : (Integer) properties
                .get(Constants.SERVICE_RANKING);
        this.properties.put(Constants.SERVICE_RANKING,
                Integer.valueOf(num == null ? 0 : num.intValue()));
        this.registration = new ServiceRegistrationImpl();
    }

    private void checkService(Object service, String[] clazzes) {
        int i = 0;
        while (i < clazzes.length) {
            try {
                if (Class.forName(clazzes[i], false,
                        service.getClass().getClassLoader()).isInstance(service)) {
                    i++;
                } else {
                    throw new IllegalArgumentException("Service "
                            + service.getClass().getName()
                            + " does not implement the interface " + clazzes[i]);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Interface " + clazzes[i]
                        + " implemented by service " + service.getClass().getName()
                        + " cannot be located: " + e.getMessage());
            }
        }
    }

    void invalidate() {
        this.service = null;
        this.useCounters.clear();
        this.bundle = null;
        this.registration = null;
        if (this.cachedServices != null) {
            this.cachedServices = null;
        }
        String[] propertyKeys = getPropertyKeys();
        for (Object remove : propertyKeys) {
            this.properties.remove(remove);
        }
    }

    @Override
    public Bundle getBundle() {
        return this.bundle;
    }

    @Override
    public Object getProperty(String key) {
        Object value = this.properties.get(key);
        if (value != null) {
            return value;
        }
        value = this.properties.get(key.toLowerCase(Locale.US));
        if (value != null) {
            return value;
        }

        Enumeration<String> keys = this.properties.keys();
        while (keys.hasMoreElements()) {
            String localKey = keys.nextElement();
            if (localKey.equalsIgnoreCase(key)) {
                value = this.properties.get(localKey);
                break;
            }
        }

        return value;
    }

    @Override
    public String[] getPropertyKeys() {
        ArrayList<String> arrayList = new ArrayList<String>(this.properties.size());
        Enumeration<String> keys = this.properties.keys();
        while (keys.hasMoreElements()) {
            arrayList.add(keys.nextElement());
        }
        return arrayList.toArray(new String[arrayList.size()]);
    }

    @Override
    public Bundle[] getUsingBundles() {
        Bundle[] usingBundles;
        synchronized (this.useCounters) {
            if (this.useCounters.isEmpty()) {
                usingBundles = null;
            } else {
                usingBundles = this.useCounters.keySet().toArray(
                        new Bundle[this.useCounters.size()]);
            }
        }
        return usingBundles;
    }

    Object getService(Bundle bundle) {
        if (this.service == null) {
            return null;
        }
        synchronized (this.useCounters) {
            Object valueOf;
            Integer num = this.useCounters.get(bundle);
            if (num == null) {
                valueOf = Integer.valueOf(1);
            } else {
                valueOf = Integer.valueOf(num.intValue() + 1);
            }
            this.useCounters.put(bundle, (Integer) valueOf);
            if (this.isServiceFactory) {
                if (this.cachedServices == null) {
                    this.cachedServices = new HashMap<Bundle, Object>();
                }
                valueOf = this.cachedServices.get(bundle);
                if (valueOf != null) {
                    return valueOf;
                }
                try {
                    Object service = ((ServiceFactory) this.service)
                            .getService(bundle, this.registration);
                    checkService(service,
                            (String[]) this.properties
                                    .get(Constants.OBJECTCLASS));
                    this.cachedServices.put(bundle, service);
                    return service;
                } catch (Throwable e) {
                    Framework.notifyFrameworkListeners(2, null, e);
                    return null;
                }
            }
            valueOf = this.service;
            return valueOf;
        }
    }

    boolean ungetService(Bundle bundle) {
        synchronized (this.useCounters) {
            if (this.service == null) {
                return false;
            }
            Integer num = this.useCounters.get(bundle);
            if (num == null) {
                return false;
            } else if (num.intValue() == 1) {
                this.useCounters.remove(bundle);
                if (this.isServiceFactory) {
                    ((ServiceFactory) this.service).ungetService(bundle,
                            this.registration, this.cachedServices.get(bundle));
                    this.cachedServices.remove(bundle);
                }
                return false;
            } else {
                this.useCounters.put(bundle,
                        Integer.valueOf(num.intValue() - 1));
                return true;
            }
        }
    }

    @Override
    public String toString() {
        return "ServiceReference{" + this.service + "}";
    }


}
