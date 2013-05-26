package io.pdef;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

public class ObservableValue<T> {
	private T value;
	private Exception e;
	private Observer<T> observer;
	private State state;
	private final Observable<T> observable;

	public static <T> ObservableValue<T> async() {
		return new ObservableValue<T>();
	}

	public static <T> Observable<T> immediate(final T value) {
		return new ObservableValue<T>().set(value).getObservable();
	}

	public static <T> Observable<T> failed(final Exception e) {
		return new ObservableValue<T>().setException(e).getObservable();
	}

	private ObservableValue() {
		state = State.NEW;
		observable = Observable.create(new Func1<Observer<T>, Subscription>() {
			@Override
			public Subscription call(final Observer<T> observer) {
				setObserver(observer);
				return Subscriptions.empty();
			}
		});
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(state)
				.toString();
	}

	public synchronized ObservableValue<T> set(final T value) {
		checkState(state == State.NEW, "%s is not in a new state", this);
		this.state = State.SET;
		this.value = value;
		callObserver();
		return this;
	}

	public synchronized ObservableValue<T> setException(final Exception e) {
		checkState(state == State.NEW, "%s is not a new state", this);
		this.state = State.EXCEPTION;
		this.e = e;
		callObserver();
		return this;
	}

	private synchronized void setObserver(final Observer<T> observer1) {
		checkState(observer == null, "observer is already set in %s to %s", this, observer);
		observer = checkNotNull(observer1);
		callObserver();
	}

	public Observable<T> getObservable() {
		return observable;
	}

	private void callObserver() {
		if (observer == null) return;

		switch (state) {
			case SET:
				observer.onNext(value);
				observer.onCompleted();
				break;
			case EXCEPTION:
				observer.onError(e);
				observer.onCompleted();
				break;
		}
	}

	static enum State {
		NEW, SET, EXCEPTION
	}
}
