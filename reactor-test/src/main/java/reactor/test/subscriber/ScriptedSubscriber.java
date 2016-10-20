/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.test.subscriber;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Fuseable;
import reactor.test.scheduler.VirtualTimeScheduler;

/**
 * Subscriber implementation that verifies pre-defined expectations as part of its
 * subscription. Typical usage consists of the following steps: <ul> <li>Create a {@code
 * ScriptedSubscriber} builder using {@link #create()} or {@link #create(long)},</li>
 * <li>Set individual up value expectations using {@link StepBuilder#expectValue(Object)
 * expectValue(Object)}, {@link StepBuilder#expectNext(Object[])
 * expectNext(Object[])}, {@link StepBuilder#expectNextWith(Predicate)
 * expectNextWith(Predicate)}.</li> and/or <li>Set up subscription actions using either
 * {@link StepBuilder#thenRequest(long) thenRequest(long)} or {@link
 * StepBuilder#thenCancel() thenCancel()}. </li> <li>Build the {@code
 * ScriptedSubscriber} using {@link LastStepBuilder#expectComplete() expectComplete()},
 * {@link LastStepBuilder#expectError() expectError()}, {@link
 * LastStepBuilder#expectError(Class) expectError(Class)}, {@link
 * LastStepBuilder#expectErrorWith(Predicate) expectErrorWith(Predicate)}, or {@link
 * LastStepBuilder#thenCancel() thenCancel()}. </li> <li>Subscribe the built {@code
 * ScriptedSubscriber} to a {@code Publisher}.</li> <li>Verify the expectations using
 * either {@link #verify()} or {@link #verify(Duration)}.</li> <li>If any expectations
 * failed, an {@code AssertionError} will be thrown indicating the failures.</li> </ul>
 * <p>
 * <p>For example:
 * <pre>
 * ScriptedSubscriber&lt;String&gt; subscriber = ScriptedSubscriber.&lt;String&gt;create()
 *   .expectValue("foo")
 *   .expectValue("bar")
 *   .expectComplete();
 *
 * Publisher&lt;String&gt; publisher = Flux.just("foo", "bar");
 * publisher.subscribe(subscriber);
 *
 * subscriber.verify();
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Stephane Maldini
 * @since 1.0
 */
public interface ScriptedSubscriber<T> extends Subscriber<T> {

	/**
	 *
	 */
	static void enableVirtualTime() {
		enableVirtualTime(false);
	}

	/**
	 * @param allSchedulers
	 */
	static void enableVirtualTime(boolean allSchedulers) {
		VirtualTimeScheduler.enable(allSchedulers);
	}

	/**
	 *
	 */
	static void disableVirtualTime() {
		VirtualTimeScheduler.reset();
	}

	/**
	 * Verify the signals received by this subscriber. This method will
	 * <strong>block</strong> indefinitely until the stream has been terminated (either
	 * through {@link #onComplete()}, {@link #onError(Throwable)} or {@link
	 * Subscription#cancel()}).
	 *
	 * @return the {@link Duration} of the verification
	 *
	 * @throws AssertionError in case of expectation failures
	 */
	Duration verify() throws AssertionError;

	/**
	 * Make the specified publisher subscribe to this subscriber and then verify the
	 * signals received by this subscriber. This method will <strong>block</strong>
	 * indefinitely until the stream has been terminated (either through {@link
	 * #onComplete()}, {@link #onError(Throwable)} or {@link Subscription#cancel()}).
	 *
	 * @param publisher the publisher to subscribe to
	 *
	 * @return the {@link Duration} of the verification
	 *
	 * @throws AssertionError in case of expectation failures
	 */
	Duration verify(Publisher<? extends T> publisher) throws AssertionError;

	/**
	 * Verify the signals received by this subscriber. This method will
	 * <strong>block</strong> for the given duration or until the stream has been
	 * terminated (either through {@link #onComplete()}, {@link #onError(Throwable)} or
	 * {@link Subscription#cancel()}).
	 *
	 * @return the {@link Duration} of the verification
	 *
	 * @throws AssertionError in case of expectation failures, or when the verification
	 *                        times out
	 */
	Duration verify(Duration duration) throws AssertionError;

	/**
	 * Make the specified publisher subscribe to this subscriber and then verify the
	 * signals received by this subscriber. This method will <strong>block</strong> for
	 * the given duration or until the stream has been terminated (either through {@link
	 * #onComplete()}, {@link #onError(Throwable)} or {@link Subscription#cancel()}).
	 *
	 * @param publisher the publisher to subscribe to
	 *
	 * @return the {@link Duration} of the verification
	 *
	 * @throws AssertionError in case of expectation failures, or when the verification
	 *                        times out
	 */
	Duration verify(Publisher<? extends T> publisher, Duration duration)
			throws AssertionError;

	/**
	 * Create a new {@code ScriptedSubscriber} that requests an unbounded amount of
	 * values.
	 *
	 * @param <T> the type of the subscriber
	 *
	 * @return a builder for setting up value expectations
	 */
	static <T> FirstStepBuilder<T> create() {
		return create(Long.MAX_VALUE);
	}

	/**
	 * Create a new {@code ScriptedSubscriber} that requests a specified amount of values.
	 *
	 * @param n the amount of items to request
	 * @param <T> the type of the subscriber
	 *
	 * @return a builder for setting up value expectations
	 */
	static <T> FirstStepBuilder<T> create(long n) {
		DefaultScriptedSubscriberBuilder.checkPositive(n);
		return new DefaultScriptedSubscriberBuilder<>(n);
	}

	/**
	 * Define a builder for terminal states.
	 *
	 * @param <T> the type of values that the subscriber contains
	 */
	interface LastStepBuilder<T> {

		/**
		 * Expect an unspecified error.
		 *
		 * @return the built subscriber
		 *
		 * @see Subscriber#onError(Throwable)
		 */
		ScriptedSubscriber<T> expectError();

		/**
		 * Expect an error of the specified type.
		 *
		 * @param clazz the expected error type
		 *
		 * @return the built subscriber
		 *
		 * @see Subscriber#onError(Throwable)
		 */
		ScriptedSubscriber<T> expectError(Class<? extends Throwable> clazz);

		/**
		 * Expect an error with the specified message.
		 *
		 * @param errorMessage the expected error message
		 *
		 * @return the built subscriber
		 *
		 * @see Subscriber#onError(Throwable)
		 */
		ScriptedSubscriber<T> expectErrorMessage(String errorMessage);

		/**
		 * Expect an error and evaluate with the given predicate.
		 *
		 * @param predicate the predicate to test on the next received error
		 *
		 * @return the built subscriber
		 *
		 * @see Subscriber#onError(Throwable)
		 */
		ScriptedSubscriber<T> expectErrorWith(Predicate<Throwable> predicate);

		/**
		 * Expect an error and consume with the given consumer. Any {@code
		 * AssertionError}s thrown by the consumer will be rethrown during {@linkplain
		 * #verify() verification}.
		 *
		 * @param consumer the consumer for the exception
		 *
		 * @return the built subscriber
		 */
		ScriptedSubscriber<T> consumeErrorWith(Consumer<Throwable> consumer);

		/**
		 * Expect the completion signal.
		 *
		 * @return the built subscriber
		 *
		 * @see Subscriber#onComplete()
		 */
		ScriptedSubscriber<T> expectComplete();

		/**
		 * Cancel the underlying subscription.
		 * {@link ScriptedSubscriber#create(long)}.
		 *
		 * @return the built subscriber
		 *
		 * @see Subscription#cancel()
		 */
		ScriptedSubscriber<T> thenCancel();
	}

	/**
	 * Define a builder for expecting main sequence individual signals.
	 *
	 * @param <T> the type of values that the subscriber contains
	 */
	interface StepBuilder<T> extends LastStepBuilder<T> {

		/**
		 * @return this builder
		 */
		StepBuilder<T> advanceTime();

		/**
		 * @param timeshift
		 *
		 * @return this builder
		 */
		StepBuilder<T> advanceTimeBy(Duration timeshift);

		/**
		 * @param instant
		 *
		 * @return this builder
		 */
		StepBuilder<T> advanceTimeTo(Instant instant);

		/**
		 * Expect an element and consume with the given consumer. Any {@code
		 * AssertionError}s thrown by the consumer will be rethrown during {@linkplain
		 * #verify() verification}.
		 *
		 * @param consumer the consumer for the value
		 *
		 * @return this builder
		 */
		StepBuilder<T> consumeNextWith(Consumer<? super T> consumer);

		/**
		 * Expect the next elements received to be equal to the given values.
		 *
		 * @param ts the values to expect
		 *
		 * @return this builder
		 *
		 * @see Subscriber#onNext(Object)
		 */
		StepBuilder<T> expectNext(T... ts);

		/**
		 * Expect an element count starting from the last expectation or onSubscribe.
		 *
		 * @param count the predicate to test on the next received value
		 *
		 * @return this builder
		 *
		 * @see Subscriber#onNext(Object)
		 */
		StepBuilder<T> expectNextCount(long count);

		/**
		 * Expect an element and evaluate with the given predicate.
		 *
		 * @param predicate the predicate to test on the next received value
		 *
		 * @return this builder
		 *
		 * @see Subscriber#onNext(Object)
		 */
		StepBuilder<T> expectNextWith(Predicate<? super T> predicate);

		/**
		 * Expect the next elements to match the given {@link Iterable} until its
		 * iterator depletes.
		 *
		 * @param iterable the predicate to test on the next received value
		 *
		 * @return this builder
		 *
		 * @see Subscriber#onNext(Object)
		 */
		StepBuilder<T> expectNextAs(Iterable<? extends T> iterable);

		/**
		 * Run an arbitrary task scheduled after previous expectations or tasks.
		 *
		 * @param task the task to run
		 *
		 * @return this builder
		 */
		StepBuilder<T> then(Runnable task);

		/**
		 * Request the given amount of elements from the upstream {@code Publisher}. This
		 * is in addition to the initial number of elements requested by {@link
		 * ScriptedSubscriber#create(long)}.
		 *
		 * @param n the number of elements to request
		 *
		 * @return this builder
		 *
		 * @see Subscription#request(long)
		 */
		StepBuilder<T> thenRequest(long n);
	}

	/**
	 * Define a builder for explicitly expecting an initializing {@link Subscription} as
	 * first signal.
	 * <p>
	 * If {@link FirstStepBuilder} expectations are not used, the produced
	 * {@link ScriptedSubscriber} keeps a first expectation that will be checking if
	 * the first signal is a
	 * {@link Subscription}.
	 *
	 * @param <T> the type of values that the subscriber contains
	 */
	interface FirstStepBuilder<T> extends StepBuilder<T> {

		/**
		 * Expect a {@link Subscription} and consume with the given consumer. Any {@code
		 * AssertionError}s thrown by the consumer will be rethrown during {@linkplain
		 * #verify() verification}.
		 *
		 * @param consumer the consumer for the {@link Subscription}
		 *
		 * @return this builder
		 *
		 * @see Subscriber#onSubscribe(Subscription)
		 */
		StepBuilder<T> consumeSubscriptionWith(Consumer<? super Subscription> consumer);

		/**
		 * Expect a {@link Subscription}.
		 * Effectively behave as the default implicit {@link Subscription} expectation.
		 *
		 * @return this builder
		 *
		 * @see Subscriber#onSubscribe(Subscription)
		 */
		StepBuilder<T> expectSubscription();

		/**
		 * Expect a {@link Subscription} and evaluate with the given predicate.
		 *
		 * @param predicate the predicate to test on the received {@link Subscription}
		 *
		 * @return this builder
		 *
		 * @see Subscriber#onSubscribe(Subscription)
		 */
		StepBuilder<T> expectSubscriptionWith(Predicate<? super Subscription> predicate);

		/**
		 * Expect the source {@link Publisher} to run with Reactor Fusion flow
		 * optimization. It will be requesting {@link Fuseable#ANY} fusion mode.
		 *
		 * @return this builder
		 *
		 * @see Fuseable
		 */
		StepBuilder<T> expectFusion();

		/**
		 * Expect the source {@link Publisher} to run the requested Reactor Fusion mode
		 * from any of these modes :
		 * {@link Fuseable#NONE}, {@link Fuseable#SYNC}, {@link Fuseable#ASYNC},
		 * {@link Fuseable#ANY}, {@link Fuseable#THREAD_BARRIER}.
		 *
		 * @param requested the requested and expected fusion mode
		 *
		 * @return this builder
		 *
		 * @see Fuseable
		 */
		StepBuilder<T> expectFusion(int requested);

		/**
		 * Expect the source {@link Publisher} to run with Reactor Fusion flow
		 * optimization.
		 * Expect the source {@link Publisher} to run the requested Reactor Fusion mode
		 * from any of these modes :
		 * {@link Fuseable#NONE}, {@link Fuseable#SYNC}, {@link Fuseable#ASYNC},
		 * {@link Fuseable#ANY}, {@link Fuseable#THREAD_BARRIER}.
		 *
		 * @param requested the requested fusion mode
		 * @param expected the expected fusion mode
		 *
		 * @return this builder
		 *
		 * @see Fuseable
		 */
		StepBuilder<T> expectFusion(int requested, int expected);
	}
}
