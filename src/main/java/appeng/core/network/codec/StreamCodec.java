/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.network.codec;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;

/**
 * Minimal backport of Minecraft 1.20.5's StreamCodec (network serialization).
 */
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B buf) {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.encode(buf, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B buf) {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.encode(value, buf);
            }
        };
    }

    static <B, V> StreamCodec<B, V> unit(V value) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B buf) {
                return value;
            }

            @Override
            public void encode(B buf, V v) {
                if (!v.equals(value)) {
                    throw new IllegalStateException("Can't encode '" + v + "', expected '" + value + "'");
                }
            }
        };
    }

    default <O> StreamCodec<B, O> apply(CodecOperation<B, V, O> operation) {
        return operation.apply(this);
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        var self = this;
        return new StreamCodec<B, O>() {
            @Override
            public O decode(B buf) {
                return to.apply(self.decode(buf));
            }

            @Override
            public void encode(B buf, O value) {
                self.encode(buf, from.apply(value));
            }
        };
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>) this;
    }

    default <U> StreamCodec<B, U> dispatch(Function<? super U, ? extends V> typeGetter,
            Function<? super V, ? extends StreamCodec<? super B, ? extends U>> codecGetter) {
        var self = this;
        return new StreamCodec<B, U>() {
            @Override
            public U decode(B buf) {
                V type = self.decode(buf);
                return codecGetter.apply(type).decode(buf);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void encode(B buf, U value) {
                V type = typeGetter.apply(value);
                var codec = (StreamCodec<B, U>) codecGetter.apply(type);
                self.encode(buf, type);
                codec.encode(buf, value);
            }
        };
    }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            Function<T1, C> factory) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B buf) {
                return factory.apply(c1.decode(buf));
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
            }
        };
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            java.util.function.BiFunction<T1, T2, C> factory) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 a = c1.decode(buf);
                T2 b = c2.decode(buf);
                return factory.apply(a, b);
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            Function3<T1, T2, T3, C> factory) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 a = c1.decode(buf);
                T2 b = c2.decode(buf);
                T3 c = c3.decode(buf);
                return factory.apply(a, b, c);
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
                c3.encode(buf, g3.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            Function4<T1, T2, T3, T4, C> factory) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 a = c1.decode(buf);
                T2 b = c2.decode(buf);
                T3 c = c3.decode(buf);
                T4 d = c4.decode(buf);
                return factory.apply(a, b, c, d);
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
                c3.encode(buf, g3.apply(value));
                c4.encode(buf, g4.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            StreamCodec<? super B, T5> c5, Function<C, T5> g5,
            Function5<T1, T2, T3, T4, T5, C> factory) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 a = c1.decode(buf);
                T2 b = c2.decode(buf);
                T3 c = c3.decode(buf);
                T4 d = c4.decode(buf);
                T5 e = c5.decode(buf);
                return factory.apply(a, b, c, d, e);
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
                c3.encode(buf, g3.apply(value));
                c4.encode(buf, g4.apply(value));
                c5.encode(buf, g5.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1,
            Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            StreamCodec<? super B, T5> c5, Function<C, T5> g5,
            StreamCodec<? super B, T6> c6, Function<C, T6> g6,
            Function6<T1, T2, T3, T4, T5, T6, C> factory) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B buf) {
                T1 a = c1.decode(buf);
                T2 b = c2.decode(buf);
                T3 c = c3.decode(buf);
                T4 d = c4.decode(buf);
                T5 e = c5.decode(buf);
                T6 f = c6.decode(buf);
                return factory.apply(a, b, c, d, e, f);
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
                c3.encode(buf, g3.apply(value));
                c4.encode(buf, g4.apply(value));
                c5.encode(buf, g5.apply(value));
                c6.encode(buf, g6.apply(value));
            }
        };
    }

    static <B, T> StreamCodec<B, T> recursive(UnaryOperator<StreamCodec<B, T>> factory) {
        return new StreamCodec<B, T>() {
            private final Supplier<StreamCodec<B, T>> inner = com.google.common.base.Suppliers
                    .memoize(() -> factory.apply(this));

            @Override
            public T decode(B buf) {
                return inner.get().decode(buf);
            }

            @Override
            public void encode(B buf, T value) {
                inner.get().encode(buf, value);
            }
        };
    }

    @FunctionalInterface
    interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }

    @FunctionalInterface
    interface StreamMemberEncoder<B, V> {
        void encode(V value, B buf);
    }

    static <B, V> BiConsumer<B, V> encoderOf(StreamCodec<B, V> codec) {
        return codec::encode;
    }
}
