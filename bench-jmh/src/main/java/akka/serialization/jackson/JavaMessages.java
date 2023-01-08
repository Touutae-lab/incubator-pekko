/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2019-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.serialization.jackson;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class JavaMessages {
  interface JTestMessage {}

  public static class JSmall implements JTestMessage {
    public final String name;
    public final int num;

    public JSmall(String name, int num) {
      this.name = name;
      this.num = num;
    }
  }

  public static class JMedium implements JTestMessage {
    public final String field1;
    public final String field2;
    public final String field3;
    public final int num1;
    public final int num2;
    public final int num3;
    public final boolean flag1;
    public final boolean flag2;
    public final Duration duration;

    public final LocalDateTime date;
    public final Instant instant;
    public final JSmall nested1;
    public final JSmall nested2;
    public final JSmall nested3;

    public JMedium(
        String field1,
        String field2,
        String field3,
        int num1,
        int num2,
        int num3,
        boolean flag1,
        boolean flag2,
        Duration duration,
        LocalDateTime date,
        Instant instant,
        JSmall nested1,
        JSmall nested2,
        JSmall nested3) {
      this.field1 = field1;
      this.field2 = field2;
      this.field3 = field3;
      this.num1 = num1;
      this.num2 = num2;
      this.num3 = num3;
      this.flag1 = flag1;
      this.flag2 = flag2;
      this.duration = duration;
      this.date = date;
      this.instant = instant;
      this.nested1 = nested1;
      this.nested2 = nested2;
      this.nested3 = nested3;
    }
  }

  public static class JLarge implements JTestMessage {
    public final JMedium nested1;
    public final JMedium nested2;
    public final JMedium nested3;
    public final List<JMedium> list;
    public final Map<String, JMedium> map;

    public JLarge(
        JMedium nested1,
        JMedium nested2,
        JMedium nested3,
        List<JMedium> list,
        Map<String, JMedium> map) {
      this.nested1 = nested1;
      this.nested2 = nested2;
      this.nested3 = nested3;
      this.list = list;
      this.map = map;
    }
  }
}
