package org.dv.saves.main

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

class Store<T> {

    private val storeSubject: Subject<T>

    constructor() {
        this.storeSubject = BehaviorSubject.create<T>().toSerialized()
    }

    constructor(defaultValue: T) {
        this.storeSubject = BehaviorSubject.createDefault(defaultValue).toSerialized()
    }

    fun observe(): Observable<T> {
        return storeSubject.distinctUntilChanged()
    }

    fun publish(value: T) {
        storeSubject.onNext(value)
    }
}