package org.dv.saves.extensions

import com.github.thomasnield.rxkotlinfx.observeOnFx
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.dv.saves.main.Store

fun <T> Observable<T>.subscribe(store: Store<T>): Disposable {
    return this.subscribe { store.publish(it) }
}

fun <T> Observable<T>.toObservableList(): ObservableList<T> {
    val list = FXCollections.observableArrayList<T>()
    this.observeOnFx()
            .subscribe {
                list.add(it)
            }
    return list
}