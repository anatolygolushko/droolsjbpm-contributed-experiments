/*
 * Copyright 2016 Red Hat Inc.
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
package org.drools.persistence.kie.persistence.session;

import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;

import org.drools.compiler.Person;
import org.drools.persistence.mapdb.util.MapDBPersistenceUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import com.arjuna.ats.jta.TransactionManager;

public class PersistentSessionForallTest {

    private KieSession kieSession;

    private Map<String, Object> context;
    private Environment env;
    
    @BeforeClass
    public static void configureTx() {
        try {
            InitialContext initContext = new InitialContext();

            initContext.rebind("java:comp/UserTransaction", com.arjuna.ats.jta.UserTransaction.userTransaction());
            initContext.rebind("java:comp/TransactionManager", TransactionManager.transactionManager());
            initContext.rebind("java:comp/TransactionSynchronizationRegistry", new com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setUp() throws Exception {
        setupPersistence();
        createKieSession();
    }

    private void setupPersistence() {
        context = MapDBPersistenceUtil.setupMapDB();
        env = MapDBPersistenceUtil.createEnvironment(context);
    }

    private void createKieSession() {
        String drl =
                "import " + Person.class.getCanonicalName() + "\n" +
                "import " + Pet.class.getCanonicalName() + "\n" +
                "\n" +
                "import java.util.ArrayList;\n" +
                "\n" +
                "// all people known as \"cat lady\" have only cats as pets\n" +
                "rule \"Forall1\" when\n" +
                "  forall ( $pet : Pet ( owner.name  == 'cat lady' )\n" +
                "           Pet ( this == $pet, type == Pet.PetType.cat )\n" +
                "    )\n" +
                "then\n" +
                "end\n" +
                "\n" +
                "// all people known as \"dog lady\" have only dogs as pets\n" +
                "rule \"Forall2\" when\n" +
                "  forall ( $pet : Pet ( owner.name == 'dog lady')\n" +
                "           Pet ( this == $pet, type == Pet.PetType.dog )\n" +
                "  )\n" +
                "then\n" +
                "end\n";

        KieBase kbase = new KieHelper().addContent( drl, ResourceType.DRL ).build();
        kieSession = KieServices.Factory.get().getStoreServices().newKieSession( kbase, null, env );
    }

    @After
    public void tearDown() throws Exception {
        cleanUpKieSession();
        MapDBPersistenceUtil.cleanUp(context);
    }

    private void cleanUpKieSession() {
        if (kieSession != null) {
            kieSession.destroy();
        }
    }

    /**
     * Tests marshalling of persistent KieSession with forall.
     */
    @Test
    public void testNotMatchedCombination() {
        TrackingAgendaEventListener listener = new TrackingAgendaEventListener();
        kieSession.addEventListener(listener);

        Person owner = new Person("cat lady"); 
        Pet dog = new Pet(Pet.PetType.dog, owner);

        kieSession.insert(dog);
        kieSession.fireAllRules();

        assertTrue(listener.isRuleFired("Forall2"));
    }

    public static class Pet implements Serializable {

        private static final long serialVersionUID = -3519777750853629395L;

        public enum PetType {
            dog, cat
        }

        private PetType type;
        private int age;
        private Person owner;

        public Pet(PetType type) {
            this.type = type;
            age = 0;
        }

        public Pet(PetType type, Person owner) {
            this(type);
            this.owner = owner;
        }

        public PetType getType() {
            return type;
        }

        public void setType(PetType type) {
            this.type = type;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Person getOwner() {
            return owner;
        }

        public void setOwner(Person owner) {
            this.owner = owner;
        }
    }

    /**
     * Listener tracking number of rules fired.
     */
    public static class TrackingAgendaEventListener extends DefaultAgendaEventListener {

        private Map<String, Integer> rulesFired = new HashMap<String, Integer>();

        @Override
        public void afterMatchFired(AfterMatchFiredEvent event) {
            String rule = event.getMatch().getRule().getName();
            if (isRuleFired(rule)) {
                rulesFired.put(rule, rulesFired.get(rule) + 1);
            } else {
                rulesFired.put(rule, 1);
            }
        }

        /**
         * Return true if the rule was fired at least once
         *
         * @param rule - name of the rule
         * @return true if the rule was fired
         */
        public boolean isRuleFired(String rule) {
            return rulesFired.containsKey(rule);
        }

        /**
         * Returns number saying how many times the rule was fired
         *
         * @param rule - name of the rule
         * @return number how many times rule was fired, 0 if rule wasn't fired
         */
        public int ruleFiredCount(String rule) {
            if (isRuleFired(rule)) {
                return rulesFired.get(rule);
            } else {
                return 0;
            }
        }

        public void clear() {
            rulesFired.clear();
        }
    }
}
