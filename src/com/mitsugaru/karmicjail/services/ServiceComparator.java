package com.mitsugaru.karmicjail.services;

import java.util.Comparator;

/**
 * Comparator for service classes.
 *
 * @param <T> Should be just Object
 */
public class ServiceComparator<T> implements Comparator<T> {

    @Override
    public int compare(T o1, T o2) {
        int compare = 0;
        //First compare by order, if available.
        Service service1 = o1.getClass().getAnnotation(Service.class);
        Service service2 = o2.getClass().getAnnotation(Service.class);
        Order order1 = Order.NORMAL;
        Order order2 = Order.NORMAL;
        if(service1 != null) {
            order1 = service1.order();
        }
        if(service2 != null) {
            order2 = service2.order();
        }
        compare = order1.ordinal() - order2.ordinal();
        
        //If they're the same order, just go by class name
        if(compare == 0) {
            compare = o1.getClass().getName().compareTo(o2.getClass().getName());
        }
        return compare;
    }

}
