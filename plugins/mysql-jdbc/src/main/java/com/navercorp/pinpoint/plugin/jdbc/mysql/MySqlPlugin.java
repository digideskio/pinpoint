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
package com.navercorp.pinpoint.plugin.jdbc.mysql;

import java.security.ProtectionDomain;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.PinpointClassFileTransformer;
import com.navercorp.pinpoint.bootstrap.interceptor.group.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.interceptor.group.InterceptorGroup;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.PreparedStatementBindingMethodFilter;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * @author Jongho Moon
 *
 */
public class MySqlPlugin implements ProfilerPlugin {

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        MySqlConfig config = new MySqlConfig(context.getConfig());
        
        addConnectionTransformer(context, config);
        addDriverTransformer(context);
        addStatementTransformer(context);
        addPreparedStatementTransformer(context, config);
        
        // From MySQL driver 5.1.x, backward compatibility is broken.
        // Driver returns not com.mysql.jdbc.Connection but com.mysql.jdbc.JDBC4Connection which extends com.mysql.jdbc.ConnectionImpl from 5.1.x
        addJDBC4PreparedStatementTransformer(context);
    }
    
    private void addConnectionTransformer(ProfilerPluginSetupContext setupContext, final MySqlConfig config) {
        PinpointClassFileTransformer transformer = new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                if (!target.isInterceptable()) {
                    return null;
                }
                
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.DatabaseInfoAccessor");

                target.addInterceptor("com.navercorp.pinpoint.plugin.jdbc.mysql.interceptor.MySQLConnectionCreateInterceptor");
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.ConnectionCloseInterceptor", MySqlConstants.GROUP_NAME);
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.StatementCreateInterceptor", MySqlConstants.GROUP_NAME);
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.PreparedStatementCreateInterceptor", MySqlConstants.GROUP_NAME);
                
                if (config.isProfileSetAutoCommit()) {
                    target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.TransactionSetAutoCommitInterceptor", MySqlConstants.GROUP_NAME);
                }
                
                if (config.isProfileCommit()) {
                    target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.TransactionCommitInterceptor", MySqlConstants.GROUP_NAME);
                }
                
                if (config.isProfileRollback()) {
                    target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.TransactionRollbackInterceptor", MySqlConstants.GROUP_NAME);
                }
                
                return target.toBytecode();
            }
        };
        
        setupContext.addClassFileTransformer("com.mysql.jdbc.Connection", transformer);
        setupContext.addClassFileTransformer("com.mysql.jdbc.ConnectionImpl", transformer);
    }
    
    private void addDriverTransformer(ProfilerPluginSetupContext setupContext) {
        setupContext.addClassFileTransformer("com.mysql.jdbc.NonRegisteringDriver", new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);

                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.DriverConnectInterceptor", va(new MySqlJdbcUrlParser(), false), MySqlConstants.GROUP_NAME, ExecutionPolicy.ALWAYS);
                
                return target.toBytecode();
            }
        });
    }
    
    private void addPreparedStatementTransformer(ProfilerPluginSetupContext setupContext, final MySqlConfig config) {
        setupContext.addClassFileTransformer("com.mysql.jdbc.PreparedStatement", new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.DatabaseInfoAccessor");
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.ParsingResultAccessor");
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.BindValueAccessor", "new java.util.HashMap()");
                
                int maxBindValueSize = config.getMaxSqlBindValueSize();

                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.PreparedStatementExecuteQueryInterceptor", va(maxBindValueSize), MySqlConstants.GROUP_NAME);
                final PreparedStatementBindingMethodFilter excludes = PreparedStatementBindingMethodFilter.excludes("setRowId", "setNClob", "setSQLXML");
                target.addGroupedInterceptor(excludes, "com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.PreparedStatementBindVariableInterceptor", MySqlConstants.GROUP_NAME, ExecutionPolicy.BOUNDARY);
                
                return target.toBytecode();
            }
        });
    }

    private void addJDBC4PreparedStatementTransformer(ProfilerPluginSetupContext setupContext) {
        setupContext.addClassFileTransformer("com.mysql.jdbc.JDBC4PreparedStatement", new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);

                final PreparedStatementBindingMethodFilter includes = PreparedStatementBindingMethodFilter.includes("setRowId", "setNClob", "setSQLXML");
                target.addGroupedInterceptor(includes, "com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.PreparedStatementBindVariableInterceptor", MySqlConstants.GROUP_NAME, ExecutionPolicy.BOUNDARY);
                
                return target.toBytecode();
            }
        });
    }

    
    private void addStatementTransformer(ProfilerPluginSetupContext setupContext) {
        PinpointClassFileTransformer transformer = new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                if (!target.isInterceptable()) {
                    return null;
                }
                
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.DatabaseInfoAccessor");

                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.StatementExecuteQueryInterceptor", MySqlConstants.GROUP_NAME);
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.StatementExecuteUpdateInterceptor", MySqlConstants.GROUP_NAME);
                
                return target.toBytecode();
            }
        };
        
        setupContext.addClassFileTransformer("com.mysql.jdbc.Statement", transformer);
        setupContext.addClassFileTransformer("com.mysql.jdbc.StatementImpl", transformer);
    }
}
