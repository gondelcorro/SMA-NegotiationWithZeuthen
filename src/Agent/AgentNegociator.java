package Agent;

import Behaviour.FSM_Negotiation;
import Concepts.Movie;
import Ontology.MCPOntology;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class AgentNegociator extends Agent {

    private AID AgentReceiverID;
    private HashMap<String, Float> pelisVotadas; //se supone vistas
    private HashMap<String, Float> utilidades;
    private ArrayList<Movie> pelis; //todas las pelis 
    private ArrayList<String> propuestasDisponibles; // no vistas
    private Boolean inicio = false;
    private String propuestaActual;

    protected void setup() {
        System.out.println("*** Bienvenido agente " + this.getLocalName() + " ***");
        try {
            getContentManager().registerLanguage(MCPOntology.getCodecInstance());
            getContentManager().registerOntology(MCPOntology.getInstance());
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("negociacion");
            sd.setName("peliculas");
            dfd.addServices(sd);

            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(new Long(1));

            this.loadPelis();
            this.loadPelisVotadas();
            this.loadPropuestasDisp();
            this.loadUtilidades();
            this.propuestaActual = this.elegirPropuesta(); //elegir la propuesta inicial segun MCP!

            DFAgentDescription[] result = DFService.search(this, dfd);
            if (result.length > 0) {
                System.out.println("HAY SERVICIO - ME SUSCRIBO");
                this.addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), dfd, sc)) {
                    @Override
                    protected void handleInform(ACLMessage inform) {
                        try {
                            DFAgentDescription[] result = DFService.decodeNotification(inform.getContent());
                            if (result.length > 0) {
                                AgentReceiverID = result[0].getName();
                                addBehaviour(new FSM_Negotiation(AgentReceiverID, inicio, propuestasDisponibles));
                            }
                        } catch (FIPAException ex) {
                            System.out.println("error en la suscripcion al servicio: " + ex);
                        }
                    }
                });
            } else {
                System.out.println("NO HAY SERVICIO - PUBLICO");
                DFService.register(this, dfd);
                inicio = true;
                this.addBehaviour(new FSM_Negotiation(this.getAID(), inicio, propuestasDisponibles));
            }
        } catch (FIPAException ex) {
            System.out.println("error buscando el servicio: " + ex);
        }
    }

    @Override
    protected void takeDown() {
       /* if (!suscripcionCancelada) {
            try {
                DFService.deregister(this);
                suscripcionCancelada = true;
                System.out.println("Agente " + getAID().getName() + " cancela su servicio.");
            } catch (FIPAException ex) {
                System.out.println("FIPA EXCEPTION: " + ex);
            }
        }*/
        System.out.println("Agente " + getAID().getLocalName() + " terminado.");
    }

    private void loadPelis() {
        pelis = new ArrayList<>();
        File archivo = null;
        FileReader fr = null;
        BufferedReader br = null;
        try {
            archivo = new File("CatalogoPeliculas.txt");
            fr = new FileReader(archivo);
            br = new BufferedReader(fr);
            String linea;
            while ((linea = br.readLine()) != null) {
                Movie movie = new Movie();
                movie.setName(linea);
                pelis.add(movie);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fr) {
                    fr.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private void loadPelisVotadas() {
        pelisVotadas = new HashMap<>();
        ArrayList listaRandomPeliPos = new ArrayList();
        //elijo una peli aleatoriamente de "pelis"
        for (int i = 0; i < 10; i++) {
            int randomPeliPos = (int) Math.abs(Math.random() * pelis.size());
            if (!listaRandomPeliPos.contains(randomPeliPos)) {
                listaRandomPeliPos.add(randomPeliPos);
                if (this.getAID().getLocalName().equals("A"))
                    pelisVotadas.put(pelis.get(randomPeliPos).getName(), (float) Math.abs(Math.random() * 1));
                else
                    pelisVotadas.put(pelis.get(randomPeliPos).getName(), (float) Math.abs(Math.random() * 5));
            }
        }
        System.out.println("Sus 10 pelis votadas son:");
        pelisVotadas.forEach((k, v) -> System.out.print("Peli: " + k + " Rating: " + v + " - "));
    }

    private void loadPropuestasDisp() {
        this.propuestasDisponibles = new ArrayList<>();
        for (Movie peli : pelis) {
            if (!pelisVotadas.containsKey(peli.getName())) {
                propuestasDisponibles.add(peli.getName());
            }
        }
        Collections.shuffle(propuestasDisponibles);//Randomiza la lista
        System.out.println("\nPelis disponibles: " + propuestasDisponibles);
    }

    private void loadUtilidades() {
        utilidades = new HashMap<>();
        for (Movie peli : pelis) {
            if (pelisVotadas.containsKey(peli.getName())) {
                utilidades.put(peli.getName(), pelisVotadas.get(peli.getName()));// el nom de la peli y el rating q se le puso xq ya esta votada
            } else {
                utilidades.put(peli.getName(), (float) Math.abs(Math.random() * (5))); //si la peli no fue votada le genero aleatoriamente un rating
            }
        }
    }

    public String elegirPropuesta() { // La eleccion es tomar la 1era de pelisDisponibles
        if (!this.propuestasDisponibles.isEmpty()) {
            propuestaActual = this.propuestasDisponibles.remove(0);
            return propuestaActual;
        }
        return null;
    }

    public boolean aceptaPropuesta(String peli) {
        System.out.println("Agente " + this.getLocalName() + ": UPeliPropuesta(" + peli + ")= " + this.getUtilidad(peli) + " | UMiPropuesta(" + propuestaActual + ")=" + this.getUtilidad(this.propuestaActual));
        return (this.getUtilidad(peli) >= this.getUtilidad(this.propuestaActual));
    }

    public float getUtilidad(String peli) {
        return utilidades.get(peli);
    }

    public String getPropuestaActual() {
        return this.propuestaActual;
    }
}
