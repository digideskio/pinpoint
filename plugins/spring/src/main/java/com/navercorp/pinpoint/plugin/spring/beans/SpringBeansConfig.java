/**
 * Copyright 2014 NAVER Corp.
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
package com.navercorp.pinpoint.plugin.spring.beans;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

/**
 * @author Jongho Moon
 *
 */
public class SpringBeansConfig {
    public static final String SPRING_BEANS_ANNOTATION = "profiler.spring.beans.annotation";
    public static final String SPRING_BEANS_CLASS_PATTERN = "profiler.spring.beans.class.pattern";
    public static final String SPRING_BEANS_NAME_PATTERN = "profiler.spring.beans.name.pattern";

    private final String springBeansNamePatterns;
    private final String springBeansClassPatterns;
    private final String springBeansAnnotations;

    public SpringBeansConfig(ProfilerConfig config) {
        this.springBeansNamePatterns = config.readString(SPRING_BEANS_NAME_PATTERN, null);
        this.springBeansClassPatterns = config.readString(SPRING_BEANS_CLASS_PATTERN, null);
        this.springBeansAnnotations = config.readString(SPRING_BEANS_ANNOTATION, null);
    }
    
    public String getSpringBeansNamePatterns() {
        return springBeansNamePatterns;
    }

    public String getSpringBeansClassPatterns() {
        return springBeansClassPatterns;
    }

    public String getSpringBeansAnnotations() {
        return springBeansAnnotations;
    }

}
