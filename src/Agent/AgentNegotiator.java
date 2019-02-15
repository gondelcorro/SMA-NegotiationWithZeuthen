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
import java.util.logging.Level;
import java.util.logging.Logger;
import util.Utilidades;

public class AgentNegotiator extends Agent {

    private AID AgentReceiverID;
    private ArrayList<Movie> catalogoPeliculas; //lista de todas las pelis 
    private ArrayList<Utilidades> listaUtilidades; // lista utilidades peli + utildiad
    private Boolean inicio = false;
    private Movie propuestaActual; // peliculaActual del agente
    private float utilidadActual;  // utilidadActual de esa peli

    protected void setup() {
        System.out.println("*** Bienvenido agente " + this.getLocalName() + " ***");
        System.out.println("Sus utilidades son:");
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

            this.loadPeliculas();
            this.loadUtilidades();
            this.propuestaActual = this.elegirPropuesta(); //elegir la propuesta inicial segun MCP!

            DFAgentDescription[] result = DFService.search(this, dfd, sc);
            if (result.length > 0) {
                System.out.println("HAY SERVICIO - ME SUSCRIBO");
                this.addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), dfd, sc)) {
                    @Override
                    protected void handleInform(ACLMessage inform) {
                        try {
                            DFAgentDescription[] result = DFService.decodeNotification(inform.getContent());
                            if (result.length > 0) {
                                AgentReceiverID = result[0].getName();//si el agente se suscribe, setea el valor del receiver
                                addBehaviour(new FSM_Negotiation(AgentReceiverID, inicio));
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
                AgentReceiverID = this.getAID();// si el agente publica, setea su id como receiver
                this.addBehaviour(new FSM_Negotiation(this.getAID(), inicio));
            }
        } catch (FIPAException ex) {
            System.out.println("error buscando el servicio: " + ex);
        }
    }

    @Override
    protected void takeDown() {
        //checkeo q el el agente q el agente q va a deregistrar su servicio del DF, sea el receiver (el receiver siempre es el q publica)
        if (this.getAID().equals(AgentReceiverID)) {
            try {
                DFService.deregister(this);
            } catch (FIPAException e) {
                 System.out.println("Error desregistrando el servicio: " + e);
            }
        }
        System.out.println("Agente " + this.getLocalName() + ": Terminado.");
    }

    private void loadPeliculas() {
        catalogoPeliculas = new ArrayList<>();
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
                catalogoPeliculas.add(movie);
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

    private void loadUtilidades() {// utilidades tendra un rating asignado para c/pelicula del catalogo
        listaUtilidades = new ArrayList<>();
        for (Movie peli : catalogoPeliculas) {
            Utilidades utilidad = new Utilidades();
            utilidad.setMovie(peli);
            if (this.getLocalName().equals("A")) { // si es Iniciator le bajo el rating para q cuando le propangan rechace 
                float util = (float) Math.abs(Math.random() * 1);
                int aux = (int) (util * 1000);
                float result = aux / 1000f;
                utilidad.setUtilidad(result);
            } else {
                float util = (float) Math.abs(Math.random() * 5);
                int aux = (int) (util * 1000);
                float result = aux / 1000f;
                utilidad.setUtilidad(result);
            }
            listaUtilidades.add(utilidad);
        }//las utilidades deben estar ordenadas de mayor a menor (segun MCP)
        Collections.sort(listaUtilidades); // el sort ordena de manera ascendente
        Collections.reverse(listaUtilidades); // con reverse invierto el orden (descendente)
        listaUtilidades.forEach(item -> System.out.print(item.getMovie().getName() + " - " + item.getUtilidad() + " | "));
    }

    public Movie elegirPropuesta() { // La eleccion es tomar la 1era pelicula
        if (!this.listaUtilidades.isEmpty()) {
            Utilidades utilidad = this.listaUtilidades.remove(0); //la saco d la lista y guardo la movie y su utilidad
            propuestaActual = utilidad.getMovie();
            utilidadActual = utilidad.getUtilidad();
            return propuestaActual;
        }
        return null;
    }

    public boolean aceptaPropuesta(Movie peli) {// se acepta si la utilidad d la peli prop es > q la prop actual
        float utilidadPeliPropuesta = getUtilidad(peli);
        System.out.println("Agente " + this.getLocalName() + ": UPeliPropuesta(" + peli.getName() + ")= " + utilidadPeliPropuesta + " | UMiPropuesta(" + propuestaActual.getName() + ")=" + this.utilidadActual);
        return (utilidadPeliPropuesta >= this.utilidadActual); //retorna true (acepta) si la utilidad de peliPropuesta es mayor o igual a la utilidad de mi propuesta
    }

    public float getUtilidad(Movie movie) {
        float utilidad = 0;
        for (Utilidades u : listaUtilidades) {
            if (u.getMovie().equals(movie)) {
                utilidad = u.getUtilidad();
                break;
            }
        }
        return utilidad;
    }

    public float getUtilidadActual() {
        return this.utilidadActual;
    }

    public Movie getPropuestaActual() {
        return this.propuestaActual;
    }
}
