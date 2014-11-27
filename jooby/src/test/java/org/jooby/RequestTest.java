package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class RequestTest {
  public class RequestMock implements Request {

    @Override
    public MediaType type() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<MediaType> accept() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MediaType> accepts(final List<MediaType> types) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Mutant> params() throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public Mutant param(final String name) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public Mutant header(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Mutant> headers() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Cookie> cookie(final String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Cookie> cookies() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T body(final TypeLiteral<T> type) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getInstance(final Key<T> key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Charset charset() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Locale locale() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long length() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String ip() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Route route() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String hostname() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Session session() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Session> ifSession() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String protocol() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean secure() {
      throw new UnsupportedOperationException();
    }

  }

  @Test
  public void accepts() throws Exception {
    LinkedList<MediaType> dataList = new LinkedList<>();
    new RequestMock() {
      @Override
      public Optional<MediaType> accepts(final List<MediaType> types) {
        dataList.addAll(types);
        return null;
      }
    }.accepts(MediaType.json);
    assertEquals(Arrays.asList(MediaType.json), dataList);
  }

  @Test
  public void acceptsStr() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    new RequestMock() {
      @Override
      public Optional<MediaType> accepts(final List<MediaType> types) {
        dataList.addAll(types);
        return null;
      }
    }.accepts("json");
    assertEquals(Arrays.asList(MediaType.json), dataList);
  }

  @Test
  public void body() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    new RequestMock() {
      @Override
      public <T> T body(final TypeLiteral<T> type) throws Exception {
        dataList.add(type.getRawType());
        return null;
      }
    }.body(Object.class);
    assertEquals(Arrays.asList(Object.class), dataList);
  }

  @Test
  public void getInstance() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    new RequestMock() {
      @Override
      public <T> T getInstance(final Key<T> key) {
        dataList.add(key);
        return null;
      }
    }.getInstance(Object.class);
    assertEquals(Arrays.asList(Key.get(Object.class)), dataList);
  }

  @Test
  public void getTypeLiteralInstance() throws Exception {
    LinkedList<Object> dataList = new LinkedList<>();
    new RequestMock() {
      @Override
      public <T> T getInstance(final Key<T> key) {
        dataList.add(key);
        return null;
      }
    }.getInstance(TypeLiteral.get(Object.class));
    assertEquals(Arrays.asList(Key.get(Object.class)), dataList);
  }

  @Test
  public void xhr() throws Exception {
    new MockUnit(Mutant.class)
        .expect(unit -> {
          Mutant xRequestedWith = unit.get(Mutant.class);
          expect(xRequestedWith.toOptional(String.class)).andReturn(Optional.of("XMLHttpRequest"));

          expect(xRequestedWith.toOptional(String.class)).andReturn(Optional.empty());
        })
        .run(unit -> {
          assertEquals(true, new RequestMock() {
            @Override
            public Mutant header(final String name) {
              assertEquals("X-Requested-With", name);
              return unit.get(Mutant.class);
            }
          }.xhr());

          assertEquals(false, new RequestMock() {
            @Override
            public Mutant header(final String name) {
              assertEquals("X-Requested-With", name);
              return unit.get(Mutant.class);
            }
          }.xhr());
        });
  }

  @Test
  public void path() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.path()).andReturn("/path");
        })
        .run(unit -> {
          assertEquals("/path", new RequestMock() {
            @Override
            public Route route() {
              return unit.get(Route.class);
            }
          }.path());
        });
  }

  @Test
  public void verb() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.verb()).andReturn(Verb.PATCH);
        })
        .run(unit -> {
          assertEquals(Verb.PATCH, new RequestMock() {
            @Override
            public Route route() {
              return unit.get(Route.class);
            }
          }.verb());
        });
  }

}