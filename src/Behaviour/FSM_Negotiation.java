package Behaviour;

import Agent.AgentNegotiator;
import Concepts.IsMyZeuthen;
import Concepts.Movie;
import Concepts.SeeMovie;
import Ontology.MCPOntology;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.Date;

public class FSM_Negotiation extends FSMBehaviour {

    IsMyZeuthen myZeuthen = new IsMyZeuthen();
    DataStore ds = new DataStore();

    public static String RECEIVER_AID = "ReceiverID";
    //Constantes Estados
    private static final String S1A_ENVIAR_PROPUESTA = "EnviarPropuesta";
    private static final String S1B_ESPERAR_PROPUESTA = "EsperarPropuesta";
    private static final String S2_ESPERAR_RESPUESTA = "EsperarRespuesta";
    private static final String S3_CALCULAR_Y_ENVIAR_ZEUTHEN = "CalcularYEnviarZeuthen";
    private static final String S4_RECIBIR_ZEUTHEN_OPONENTE = "RecibirZeuthenOponente";
    private static final String S5_EVALUAR_PROPUESTA_Y_RESPONDER = "EvaluarPropuestaYResponder";
    private static final String S6_FINALIZAR_NEGOCIACION = "FinalizarNegociacion";

    public FSM_Negotiation(AID agentID, Boolean inicio) {

        ds.put(RECEIVER_AID, agentID); // ID "A" q publica el servicio. B le envia la 1era propuesta
        System.out.println("*** Inicia Negociacion ***");
        if (inicio) {// si no hay servicio -> publica y espera propuesta
            EsperarPropuesta s1 = new EsperarPropuesta();
            s1.setDataStore(ds);
            this.registerFirstState(s1, S1B_ESPERAR_PROPUESTA);
            //Agrego este estado para q queden todos registrados en la FSM
            EnviarPropuesta sX = new EnviarPropuesta();
            sX.setDataStore(ds);
            this.registerState(sX, S1A_ENVIAR_PROPUESTA);
        } else {
            EnviarPropuesta s1 = new EnviarPropuesta();
            s1.setDataStore(ds);
            this.registerFirstState(s1, S1A_ENVIAR_PROPUESTA);

            EsperarPropuesta sX = new EsperarPropuesta();
            sX.setDataStore(ds);
            this.registerState(sX, S1B_ESPERAR_PROPUESTA);
        }
        EsperarRespuesta s2 = new EsperarRespuesta();
        s2.setDataStore(ds);
        this.registerState(s2, S2_ESPERAR_RESPUESTA);

        CalcularYEnviarZeuthen s3 = new CalcularYEnviarZeuthen();
        s3.setDataStore(ds);
        this.registerState(s3, S3_CALCULAR_Y_ENVIAR_ZEUTHEN);

        RecibirZeuthenOponente s4 = new RecibirZeuthenOponente();
        s4.setDataStore(ds);
        this.registerState(s4, S4_RECIBIR_ZEUTHEN_OPONENTE);

        EvaluarPropuestaYResponder s5 = new EvaluarPropuestaYResponder();
        s5.setDataStore(ds);
        this.registerState(s5, S5_EVALUAR_PROPUESTA_Y_RESPONDER);

        FinalizarNegociacion s6 = new FinalizarNegociacion();
        s6.setDataStore(ds);
        this.registerLastState(s6, S6_FINALIZAR_NEGOCIACION);

        String[] toBeReset = {S4_RECIBIR_ZEUTHEN_OPONENTE};
        String[] toBeReset1 = {S1B_ESPERAR_PROPUESTA};
        String[] toBeReset2 = {S2_ESPERAR_RESPUESTA};
        this.registerDefaultTransition(S1A_ENVIAR_PROPUESTA, S2_ESPERAR_RESPUESTA);
        this.registerDefaultTransition(S1B_ESPERAR_PROPUESTA, S5_EVALUAR_PROPUESTA_Y_RESPONDER, toBeReset1); //cuando sale del comp ahi agrego el reset
        this.registerTransition(S5_EVALUAR_PROPUESTA_Y_RESPONDER, S6_FINALIZAR_NEGOCIACION, 1);
        this.registerTransition(S2_ESPERAR_RESPUESTA, S6_FINALIZAR_NEGOCIACION, 1, toBeReset2);
        this.registerTransition(S2_ESPERAR_RESPUESTA, S3_CALCULAR_Y_ENVIAR_ZEUTHEN, 0, toBeReset2);
        this.registerDefaultTransition(S3_CALCULAR_Y_ENVIAR_ZEUTHEN, S4_RECIBIR_ZEUTHEN_OPONENTE);
        this.registerTransition(S5_EVALUAR_PROPUESTA_Y_RESPONDER, S3_CALCULAR_Y_ENVIAR_ZEUTHEN, 0);
        this.registerTransition(S4_RECIBIR_ZEUTHEN_OPONENTE, S1B_ESPERAR_PROPUESTA, 5, toBeReset);
        this.registerTransition(S4_RECIBIR_ZEUTHEN_OPONENTE, S1A_ENVIAR_PROPUESTA, 1, toBeReset);
    }

    private class EnviarPropuesta extends Behaviour {

        private Boolean envio = false;
        private Boolean primeraVez = true;

        @Override
        public void action() {
            AgentNegotiator agentNegotiator = ((AgentNegotiator) myAgent);
            Movie sigPropuesta;
            if (primeraVez) {
                sigPropuesta = agentNegotiator.getPropuestaActual();
            } else {
                sigPropuesta = agentNegotiator.elegirPropuesta();
            }
            if (sigPropuesta == null) { // if == null => no hay más!
                System.out.println("NO HAY MAS PELICULAS PARA PROPONER");
            } else { //Arma el mensaje! y lo envia
                try {
                    Movie movie = new Movie();
                    movie.setName(sigPropuesta.getName());
                    SeeMovie seeMovie = new SeeMovie();
                    seeMovie.setMovie(movie);
                    seeMovie.setDate(new Date(2019, 02, 15));
                    ACLMessage ultMsg = (ACLMessage) this.getDataStore().get("msgUltimo");
                    ACLMessage msgPropuesta;
                    if (ultMsg != null) {//ya hubo negociacion -> creo respuesta al "msgUltimo"
                        msgPropuesta = ultMsg.createReply();
                        msgPropuesta.setPerformative(ACLMessage.PROPOSE);
                    } else {
                        msgPropuesta = new ACLMessage(ACLMessage.PROPOSE);
                        msgPropuesta.addReceiver((AID) this.getDataStore().get(RECEIVER_AID)); // 1era vez se envia al receiver (A xq publico el servicio)
                        msgPropuesta.setLanguage(MCPOntology.getCodecInstance().getName());
                        msgPropuesta.setOntology(MCPOntology.getInstance().getName());
                        msgPropuesta.setConversationId("negociacion-pelicula");
                        msgPropuesta.setReplyWith("propuesta" + System.currentTimeMillis());// valor unico
                    }
                    msgPropuesta.setContentObject(seeMovie);
                    myAgent.send(msgPropuesta);
                    System.out.println("Agente " + myAgent.getLocalName() + ": Envia propuesta: " + movie.getName());
                    envio = true;
                    primeraVez = false;
                } catch (IOException ex) {
                    System.out.println("Error: " + ex);
                }
            }
        }

        @Override
        public boolean done() {
            return envio;
        }
    }

    private class EsperarRespuesta extends Behaviour {

        boolean respuesta = false;
        private int acepta = 0;

        public void action() {
            ACLMessage msgRespuesta = myAgent.receive();
            if (msgRespuesta != null) {// respuesta recibida
                if (msgRespuesta.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta aceptada - FIN DE LA NEGOCIACION");
                    acepta = 1;
                    respuesta = true;
                } else {//si rechazó
                    try {
                        SeeMovie seeMovie = (SeeMovie) msgRespuesta.getContentObject();
                        this.getDataStore().put("propuestaDelOtroAgente", seeMovie.getMovie()); //Guardo la propuesta actual del otro agente en el DS
                    } catch (UnreadableException ex) {
                        System.out.println("** ERROR*** " + ex);
                    }
                    System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta rechazada... paso a informar zeuthen...");
                    this.getDataStore().put("msgUltimo", msgRespuesta); // guarda el ultimo msg recibido (msg de rechazo q contiene la prop del otro agente)
                    respuesta = true;
                }
            } else {
                block();
                System.out.println("Agente " + myAgent.getLocalName() + ": Esperando respuesta..");
            }
        }
        // Los reset agrego a todos los comportamientos q reciban msgs..
        // La FSM crea una sola instancia con los comp x lo q hay q resetear los valores
        @Override
        public void reset() {
            respuesta = false;
        }

        @Override
        public int onEnd() {
            return acepta;
        }

        @Override
        public boolean done() {
            return respuesta;
        }
    }

    private class CalcularYEnviarZeuthen extends Behaviour { // estado 3

        private Boolean envioZeuthen = false;

        @Override
        public void action() {
            try {
                AgentNegotiator agente = ((AgentNegotiator) myAgent);
                float utilidadPropActual = agente.getUtilidadActual();
                Movie movie = (Movie) this.getDataStore().get("propuestaDelOtroAgente"); // contiene la propuesta del otro agente
                float utilidadPropuestaOponente = agente.getUtilidad(movie);
                float zeuthen = (utilidadPropActual - utilidadPropuestaOponente) / utilidadPropActual;//Calculo el zeuthen

                IsMyZeuthen myZeuthen = new IsMyZeuthen();
                myZeuthen.setValue(zeuthen);
                this.getDataStore().put("miZeuthen", myZeuthen); //se guarda el zeuthen de cada agente en el DS
                System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es " + myZeuthen.getValue());

                ACLMessage ultimoMsg = (ACLMessage) this.getDataStore().get("msgUltimo"); //cada agente toma su ultimo msg
                ACLMessage msgZeuthen = ultimoMsg.createReply();
                msgZeuthen.setPerformative(ACLMessage.INFORM);
                msgZeuthen.setContentObject(myZeuthen);
                envioZeuthen = true;
                myAgent.send(msgZeuthen); // se informa el zeuthen

            } catch (IOException | NullPointerException ex) {
                System.out.println("Agente " + myAgent.getLocalName() + ": Error en el msje: " + ex);
            }
        }

        @Override
        public boolean done() {
            return envioZeuthen;
        }
    }

    private class RecibirZeuthenOponente extends Behaviour { //estado 4

        private int proxEstado;
        private boolean received = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msgZeuthen = myAgent.receive(mt);
            if (msgZeuthen != null) {
                try {
                    IsMyZeuthen zuethenOponente = (IsMyZeuthen) msgZeuthen.getContentObject();
                    IsMyZeuthen miZeuthen = (IsMyZeuthen) this.getDataStore().get(("miZeuthen"));
                    System.out.println("Agente " + myAgent.getLocalName() + ": Recibe zeuthen oponente... " + "(" + zuethenOponente.getValue() + ")");
                    if (miZeuthen.getValue() < zuethenOponente.getValue()) {
                        proxEstado = 1; // mi zeuthen es menor -> enviar propuesta
                        received = true;
                        System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es menor.. envio propuesta");
                    } else {
                        proxEstado = 5; // mi zeuthen es mayor -> esperar propuesta
                        received = true;
                        System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es mayor.. espero propuesta");
                    }
                } catch (UnreadableException ex) {
                    System.out.println("Error en el msje recibiendo zeuthen: " + ex);
                }
            } else {
                block();
                System.out.println("Agente " + myAgent.getLocalName() + ": Esperando Zeuthen oponente...");
            }
        }

        @Override
        public void reset() {
            received = false;
        }

        @Override
        public int onEnd() {
            return proxEstado;
        }

        @Override
        public boolean done() {
            return received;
        }
    }

    private class EsperarPropuesta extends Behaviour {

        Boolean esperarPropuesta = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msgPropuesta = myAgent.receive(mt);
            if (msgPropuesta != null) {
                this.getDataStore().put("msgPropuesta", msgPropuesta);//guarda la propuesta q le hicieron
                this.getDataStore().put("msgUltimo", msgPropuesta);
                System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta recibida.. paso a evaluar propusta y responder...");
                esperarPropuesta = true;
            } else {
                block();
                System.out.println("Agente " + myAgent.getLocalName() + ": Esperando propuesta...");
            }
        }

        @Override
        public void reset() {
            esperarPropuesta = false;
        }

        @Override
        public boolean done() {
            return esperarPropuesta;
        }
    }

    private class EvaluarPropuestaYResponder extends Behaviour {

        private int acepta = 0;
        private Boolean evaluarYResponder = false;

        @Override
        public void action() {
            ACLMessage propuesta = (ACLMessage) this.getDataStore().get("msgPropuesta");//seeMovie
            if (propuesta != null) {
                try {// Propuesta recibida
                    AgentNegotiator agente = ((AgentNegotiator) myAgent);
                    ACLMessage respuesta = propuesta.createReply();
                    SeeMovie seeMovie = (SeeMovie) propuesta.getContentObject();
                    Movie peliPropuesta = seeMovie.getMovie();
                    if (peliPropuesta.equals(agente.getPropuestaActual())) {//Si lo q le proponen es lo mismo q su propuesta actual acepta
                        respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        System.out.println("Agente " + myAgent.getLocalName() + ": La propuesta: " + peliPropuesta.getName() + " es igual a mi propuesta actual");
                        System.out.println("Agente " + myAgent.getLocalName() + ": PROPUESTA ACEPTADA");
                        myAgent.send(respuesta);
                        acepta = 1;
                        evaluarYResponder = true;
                    } else {
                        if (agente.aceptaPropuesta(peliPropuesta)) { //EVALUA LA UTILIDAD.
                            respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta: " + peliPropuesta.getName() + " - ACEPTADA");
                            myAgent.send(respuesta);
                            acepta = 1;
                            evaluarYResponder = true;
                        } else { // EL AGENTE RECHAZA LA PROPUESTA
                            this.getDataStore().put("propuestaDelOtroAgente", peliPropuesta); //guarda la propuesta (Movie) actual del otro agente (q viene en el msj)
                            respuesta.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            SeeMovie miPropuestaSeeMovie = new SeeMovie();
                            miPropuestaSeeMovie.setMovie(agente.getPropuestaActual());
                            respuesta.setContentObject(miPropuestaSeeMovie);//cuando rechaza la propuesta q recibe, recuerda la suya (seeMovie)
                            myAgent.send(respuesta);
                            evaluarYResponder = true;
                            System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta: " + peliPropuesta.getName() + " - RECHAZADA");
                        }
                    }
                } catch (IOException | UnreadableException ex) {
                    System.out.println("Error leyendo el contenido del msj");
                }
            } else {
                System.out.println("PROPUESTA NULL !!!");
            }
        }

        @Override
        public int onEnd() {
            return acepta;
        }

        @Override
        public boolean done() {
            return evaluarYResponder;
        }
    }

    public class FinalizarNegociacion extends OneShotBehaviour {
        public void action() {
           
           myAgent.doDelete();
        }

    }
}
