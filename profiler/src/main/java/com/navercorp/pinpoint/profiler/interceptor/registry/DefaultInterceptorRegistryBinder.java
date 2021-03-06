/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.interceptor.registry;

import com.navercorp.pinpoint.bootstrap.interceptor.registry.DefaultInterceptorRegistryAdaptor;
import com.navercorp.pinpoint.bootstrap.interceptor.registry.InterceptorRegistry;
import com.navercorp.pinpoint.bootstrap.interceptor.registry.InterceptorRegistryAdaptor;

/**
 * @author emeroad
 */
public class DefaultInterceptorRegistryBinder implements InterceptorRegistryBinder {

    private final Object lock;
    private final InterceptorRegistryAdaptor interceptorRegistryAdaptor = new DefaultInterceptorRegistryAdaptor();

    public DefaultInterceptorRegistryBinder() {
        this.lock = new Object();
    }

    @Override
    public void bind() {
        InterceptorRegistry.bind(interceptorRegistryAdaptor, lock);
    }

    @Override
    public void unbind() {
        InterceptorRegistry.unbind(lock);
    }

    public InterceptorRegistryAdaptor getInterceptorRegistryAdaptor() {
        return interceptorRegistryAdaptor;
    }

    @Override
    public String getInterceptorRegistryClassName() {
        return InterceptorRegistry.class.getName();
    }
}
