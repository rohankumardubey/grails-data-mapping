package org.grails.datastore.rx.collection

import grails.gorm.rx.collection.RxPersistentCollection
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.query.RxQuery
import rx.Observable
import rx.Subscriber
import rx.Subscription

/**
 * Represents a reactive set that can be observed in order to allow non-blocking lazy loading of associations
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@Slf4j
class RxPersistentSet extends PersistentSet implements RxPersistentCollection {
    final RxDatastoreClient datastoreClient
    final Association association

    Observable observable
    private QueryState queryState

    RxPersistentSet( RxDatastoreClient datastoreClient, Association association, Serializable associationKey, QueryState queryState = null) {
        super(association, associationKey, null)
        this.datastoreClient = datastoreClient
        this.association = association
        this.queryState = queryState
    }

    @Override
    void initialize() {
        if(initializing != null) return
        initializing = true


        def observable = toListObservable()

        log.warn("Association $association initialised using blocking operation. Consider using subscribe(..) or an eager query instead")

        addAll observable.toBlocking().first()
        initialized = true
    }

    @Override
    Observable<List> toListObservable() {
        toObservable().toList()
    }

    @Override
    Observable toObservable() {
        if(observable == null) {
            def query = ((RxDatastoreClientImplementor)datastoreClient).createQuery(childType, queryState)
            query.eq( association.inverseSide.name, associationKey )
            observable = ((RxQuery)query).findAll()
        }
        return observable
    }

    @Override
    Subscription subscribe(Subscriber subscriber) {
        return toObservable().subscribe(subscriber)
    }
}
