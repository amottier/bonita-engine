package com.bonitasoft.engine.bdm.dao.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bonitasoft.engine.bdm.dao.proxy.LazyLoader;
import com.bonitasoft.engine.bdm.dao.proxy.Proxyfier;
import com.bonitasoft.engine.bdm.proxy.assertion.ProxyAssert;
import com.bonitasoft.engine.bdm.proxy.model.TestEntity;

@RunWith(MockitoJUnitRunner.class)
public class ProxyTest {

	@Mock
    private LazyLoader lazyLoader;

    @InjectMocks
    private Proxyfier proxyfier;

    @Test
    public void should_load_object_when_method_is_lazy_and_object_is_not_loaded() throws Exception {
        TestEntity entity = proxyfier.proxify(new TestEntity());

        entity.getLazyEntity();

        verify(lazyLoader).load(any(Method.class), any(Long.class));
    }

    @Test
    public void should_return_value_when_it_has_been_already_loaded_before_proxyfication() throws Exception {
        String name = "this is a preloaded value";
        TestEntity entity = new TestEntity();
        entity.setName(name);
        entity = proxyfier.proxify(entity);

        String proxyName = entity.getName();

        verifyZeroInteractions(lazyLoader);
        assertThat(proxyName).isEqualTo(name);
    }

    @Test
    public void should_not_load_entity_when_it_has_been_already_loaded_before_proxyfication() throws Exception {
        TestEntity alreadySetEntity = new TestEntity();
        alreadySetEntity.setName("aDeepName");
        TestEntity entity = new TestEntity();
        entity.setLazyEntity(alreadySetEntity);
        entity = proxyfier.proxify(entity);

        TestEntity loadedEntity = entity.getLazyEntity();

        verifyZeroInteractions(lazyLoader);
        assertThat(loadedEntity.getName()).isEqualTo(alreadySetEntity.getName());
    }

    @Test
    public void should_not_load_object_which_has_been_already_lazy_loaded() throws Exception {
        TestEntity entity = proxyfier.proxify(new TestEntity());

        entity.getLazyEntity();
        entity.getLazyEntity();

        verify(lazyLoader, times(1)).load(any(Method.class), any(Long.class));
    }

    @Test
    public void should_not_load_object_for_a_non_lazy_loading_method() throws Exception {
        TestEntity entity = proxyfier.proxify(new TestEntity());

        entity.getEagerEntity();

        verifyZeroInteractions(lazyLoader);
    }

    @Test
    public void should_not_load_object_that_has_been_set_by_a_setter() throws Exception {
        TestEntity entity = proxyfier.proxify(new TestEntity());

        entity.setLazyEntity(null);
        entity.getLazyEntity();

        verifyZeroInteractions(lazyLoader);
    }

    @Test
    public void should_return_a_proxy_when_calling_a_getter_returning_an_entity() throws Exception {
        TestEntity entity = proxyfier.proxify(new TestEntity());

        TestEntity eagerEntity = entity.getEagerEntity();

        ProxyAssert.assertThat(eagerEntity).isAProxy();
    }

    @Test
    public void should_not_return_a_proxy_when_calling_a_getter_not_returning_an_entity() throws Exception {
        TestEntity entity = proxyfier.proxify(new TestEntity());
        entity.setName("aName");

        String name = entity.getName();

        ProxyAssert.assertThat(name).isNotAProxy();
    }

    @Test
    public void should_return_a_list_of_proxies_when_calling_a_getter_returning_a_list_of_entities() throws Exception {
        TestEntity entity = proxyfier.proxify(new TestEntity());

        List<TestEntity> entities = entity.getEagerEntities();

        for (TestEntity e : entities) {
            ProxyAssert.assertThat(e).isAProxy();
        }
    }

    @Test
    public void should_not_return_a_list_of_proxies_when_calling_a_getter_not_returning_a_list_of_entities() throws Exception {
        TestEntity entity = proxyfier.proxify(new TestEntity());

        List<String> strings = entity.getStrings();

        for (String string : strings) {
            ProxyAssert.assertThat(string).isNotAProxy();
        }
    }
}
