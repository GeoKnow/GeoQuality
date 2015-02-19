package org.aksw.geoknow.assessment.count;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

public class AveragePointsPerInstance implements GeoQualityMetric {

    private final Property property ;
    private Property Average;
    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private final String structureUri ;

    public AveragePointsPerInstance(Property p){
        this.property=p;
        this.structureUri = NAMESPACE + "metric/"+property.hashCode();
    }

    public Model generateResultsDataCube(Model inputModel) {
        Model cube = createModel();
        OntModel model = (inputModel instanceof OntModel) ? (OntModel) inputModel : ModelFactory.createOntologyModel(
                OntModelSpec.OWL_MEM, inputModel);
        int obsCount = 0;
        for (ExtendedIterator<OntClass> it=model.listClasses();it.hasNext();){
            OntClass ontClass = it.next();
            double sum=0;
            double i =0;
            for(ExtendedIterator<Individual> individualsIt = model.listIndividuals(ontClass);individualsIt.hasNext();){
               sum+=individualsIt.next().listPropertyValues(property).toSet().size();
               i++;
            }
           Resource obs = cube.createResource(structureUri+"/obs/"+obsCount,QB.Observation);
          double average = i==0 ? 0 :sum /i;
            obs.addProperty(Average, cube.createTypedLiteral(average) );
            obs.addProperty(GK.DIM.Class, ontClass);
            obsCount++;
        }
        return cube;
    }

    public Model generateResultsDataCube(String endpointUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    private Model createModel() {
        Model cubeData = ModelFactory.createDefaultModel();

        cubeData.createResource(NAMESPACE+"/structure/metric"+property.hashCode(),QB.MeasureProperty);

        Resource structure = cubeData.createResource(structureUri,QB.DataStructureDefinition);

        Resource c1 = cubeData.createResource(structure+"/c1",QB.ComponentSpecification);
        c1.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Class", "en"));
        c1.addProperty(QB.dimension, GK.DIM.Class);

        Resource c2 = cubeData.createResource(structure+"/c2",QB.ComponentSpecification);
        c2.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Average of "+property+" per Instance", "en"));
        c2.addProperty(QB.measure, Average);

        structure.addProperty(QB.component, c1);
        structure.addProperty(RDFS.label,
                cubeData.createLiteral("A Data Structure Definition for Instances Number Metric", "en"));
        structure.addProperty(QB.component, c2);


        cubeData.add(GK.MEASURE.InstanceCountStatements);
        cubeData.add(GK.DIM.ClassStatements);

        return cubeData;
    }

}
