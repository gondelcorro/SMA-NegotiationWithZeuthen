package Behaviour;

import Agent.AgentNegociator;
import Concepts.IsMyZeuthen;
import Concepts.Movie;
import Concepts.SeeMovie;
import Ontology.MCPOntology;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class FSM_Negotiation extends FSMBehaviour {

    IsMyZeuthen myZeuthen = new IsMyZeuthen();
    DataStore ds = new DataStore();

    //Constantes data store
    public static String PELICULAS_DISPONIBLES = "PeliculasDisponibles";
    public static String RECEIVER_AID = "ReceiverID";

    //Constantes Estados
    private static final String S1A_ENVIAR_PROPUESTA = "EnviarPropuesta";
    private static final String S1B_ESPERAR_PROPUESTA = "EsperarPropuesta";
    private static final String S2_ESPERAR_RESPUESTA = "EsperarRespuesta";
    private static final String S3_CALCULAR_Y_ENVIAR_ZEUTHEN = "CalcularYEnviarZeuthen";
    private static final String S4_RECIBIR_ZEUTHEN_OPONENTE = "RecibirZeuthenOponente";
    private static final String S5_EVALUAR_PROPUESTA_Y_RESPONDER = "EvaluarPropuestaYResponder";

    public FSM_Negotiation(AID agentID, Boolean inicio, ArrayList<String> peliculasDisponibles) {

        ds.put(RECEIVER_AID, agentID); // ID "B"
        ds.put(PELICULAS_DISPONIBLES, peliculasDisponibles);
        System.out.println("*** Inicia Negociacion ***");
        if (inicio) {
            EsperarPropuesta s1 = new EsperarPropuesta();
            s1.setDataStore(ds);
            this.registerFirstState(s1, S1B_ESPERAR_PROPUESTA);

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

        this.registerDefaultTransition(S1A_ENVIAR_PROPUESTA, S2_ESPERAR_RESPUESTA);
        this.registerDefaultTransition(S1B_ESPERAR_PROPUESTA, S5_EVALUAR_PROPUESTA_Y_RESPONDER);
        this.registerDefaultTransition(S2_ESPERAR_RESPUESTA, S3_CALCULAR_Y_ENVIAR_ZEUTHEN);
        this.registerDefaultTransition(S3_CALCULAR_Y_ENVIAR_ZEUTHEN, S4_RECIBIR_ZEUTHEN_OPONENTE);
        this.registerDefaultTransition(S5_EVALUAR_PROPUESTA_Y_RESPONDER, S3_CALCULAR_Y_ENVIAR_ZEUTHEN);
        String[] toBeReset = {S4_RECIBIR_ZEUTHEN_OPONENTE};
        this.registerTransition(S4_RECIBIR_ZEUTHEN_OPONENTE, S1B_ESPERAR_PROPUESTA, 5, toBeReset); //num de estado

        this.registerTransition(S4_RECIBIR_ZEUTHEN_OPONENTE, S1A_ENVIAR_PROPUESTA, 1, toBeReset); //num de estado 
    }

    private class EnviarPropuesta extends Behaviour {

        private Boolean envio = false;
        private Boolean primeraVez = true;

        @Override
        public void action() {
            AgentNegociator ag = ((AgentNegociator) myAgent);
            String sigPropuesta;
            if (primeraVez) {
                sigPropuesta = ag.getPropuestaActual();
            } else {
                sigPropuesta = ag.elegirPropuesta();

            }
            if (sigPropuesta == null) { // if == null => no hay más!
                System.out.println("NO HAY MAS PELICULAS PARA PROPONER");
            } else { //Arma el mensaje! y lo envia
                try {
                    Movie movie = new Movie();
                    movie.setName(sigPropuesta);
                    SeeMovie seeMovie = new SeeMovie();
                    seeMovie.setMovie(movie);
                    seeMovie.setDate(new Date(2018, 9, 17));
                    ACLMessage ultMsg = (ACLMessage) this.getDataStore().get("msgUltimo");
                    ACLMessage msgPropuesta;
                    if (ultMsg != null) {
                        msgPropuesta = ultMsg.createReply();
                        msgPropuesta.setPerformative(ACLMessage.PROPOSE);
                    } else {
                        msgPropuesta = new ACLMessage(ACLMessage.PROPOSE);
                        msgPropuesta.addReceiver((AID) this.getDataStore().get(RECEIVER_AID));
                        msgPropuesta.setLanguage(MCPOntology.getCodecInstance().getName());
                        msgPropuesta.setOntology(MCPOntology.getInstance().getName());
                        msgPropuesta.setConversationId("negociacion-pelicula");
                        msgPropuesta.setReplyWith("propuesta" + System.currentTimeMillis()); // valor unico
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

        public void action() {
            ACLMessage msgRespuesta = myAgent.receive();
            if (msgRespuesta != null) {// respuesta recibida
                if (msgRespuesta.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta aceptada - FIN DE LA NEGOCIACION");
                    myAgent.doDelete();
                } else { //si rechazó
                    this.getDataStore().put("propuestaDelOtroAgente", msgRespuesta.getContent()); //Guardo la propuesta actual del otro agente en el DS
                    System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta rechazada... paso a informar zeuthen...");
                    this.getDataStore().put("msgRechazo", msgRespuesta); // El msj de respuesta es un rechazo, lo guardo
                    this.getDataStore().put("msgUltimo", msgRespuesta);
                    respuesta = true;
                }
            } else {
                block();
                System.out.println("Agente " + myAgent.getLocalName() + ": Esperando respuesta..");
            }
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

            AgentNegociator agente = ((AgentNegociator) myAgent);
            float utilidadPropActual = agente.getUtilidad(agente.getPropuestaActual());
            String propuestaOponente = (String) this.getDataStore().get("propuestaDelOtroAgente");
            float utilidadPropuestaOponente = agente.getUtilidad(propuestaOponente);
            float zeuthen = (utilidadPropActual - utilidadPropuestaOponente) / utilidadPropActual;//Calculo el zeuthen
            this.getDataStore().put("miZeuthen", zeuthen);

            try {
                ACLMessage msgRechazo = (ACLMessage) this.getDataStore().get("msgUltimo"); //msgRechazo
                ACLMessage msgZeuthen = msgRechazo.createReply();
                msgZeuthen.setPerformative(ACLMessage.INFORM);
                msgZeuthen.setContentObject(zeuthen);

                this.getDataStore().put("miZeuthen", zeuthen);
                System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es " + zeuthen);
                envioZeuthen = true;
                myAgent.send(msgZeuthen);

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
                    float zuethenOponente = (float) msgZeuthen.getContentObject();
                    float miZeuthen = (float) this.getDataStore().get(("miZeuthen"));
                    System.out.println("Agente " + myAgent.getLocalName() + ": Recibe zeuthen oponente... " + "(" + zuethenOponente + ")");
                    if (miZeuthen > zuethenOponente) {
                        proxEstado = 1; // mi zeuthen es mayor -> enviar propuesta
                        received = true;
                        System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es mayor.. envio propuesta");
                    } else {
                        proxEstado = 5; // mi zeuthen es menor -> esperar propuesta
                        received = true;
                        System.out.println("Agente " + myAgent.getLocalName() + ": Mi zeuthen es menor.. espero propuesta");
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
            //proxEstado = 1;
            received = false;
        }

        @Override
        public int onEnd() {
            System.out.println("*********OnEnd: " + proxEstado);
            return proxEstado;
        }

        @Override
        public boolean done() {
            System.out.println("*********done: " + received);
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
                this.getDataStore().put("msgPropuesta", msgPropuesta);
                this.getDataStore().put("msgUltimo", msgPropuesta);
                System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta recibida.. paso a evaluar propusta y responder...");
                esperarPropuesta = true;
            } else {
                block();
                System.out.println("Agente " + myAgent.getLocalName() + ": Esperando propuesta...");
            }
        }

        @Override
        public boolean done() {
            return esperarPropuesta;
        }
    }

    private class EvaluarPropuestaYResponder extends Behaviour {

        private Boolean evaluarYResponder = false;

        @Override
        public void action() {
            ACLMessage propuesta = (ACLMessage) this.getDataStore().get("msgPropuesta");
            if (propuesta != null) {
                try {// Propuesta recibida
                    AgentNegociator agente = ((AgentNegociator) myAgent);
                    ACLMessage respuesta = propuesta.createReply();
                    SeeMovie seeMovie = (SeeMovie) propuesta.getContentObject();
                    String peliPropuesta = seeMovie.getMovie().getName();
                    if (agente.aceptaPropuesta(peliPropuesta)) { // SI EL AGENTE ACEPTA LA PROPUESTA TERMINA LA NEGOCIACION
                        respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta: " + peliPropuesta + " - ACEPTADA");
                        myAgent.send(respuesta);
                        myAgent.doDelete();
                    } else { // EL AGENTE RECHAZA LA PROPUESTA
                        this.getDataStore().put("propuestaDelOtroAgente", peliPropuesta); //guarda la propuesta actual del otro agente (q viene en el msj)
                        respuesta.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        respuesta.setContent(agente.getPropuestaActual());//cuando rechaza la propuesta q recibe, recuerda la suya
                        myAgent.send(respuesta);
                        evaluarYResponder = true;
                        this.getDataStore().put("msgRechazo", respuesta); // El msj de respuesta es un rechazo, lo guardo
                        System.out.println("Agente " + myAgent.getLocalName() + ": Propuesta: " + peliPropuesta + " - RECHAZADA");
                    }
                } catch (UnreadableException ex) {
                    System.out.println("Error leyendo el contenido del msj");
                }
            } else {
                System.out.println("PROPUESTA NULL !!!");
                block();
            }
        }

        @Override
        public boolean done() {
            return evaluarYResponder;
        }
    }
}
