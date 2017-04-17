/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.gsgp.population.operator;

import edu.gsgp.utils.MersenneTwister;
import edu.gsgp.utils.Utils.DatasetType;
import edu.gsgp.experiment.data.Dataset;
import edu.gsgp.experiment.data.ExperimentalData;
import edu.gsgp.experiment.data.Instance;
import edu.gsgp.experiment.config.PropertiesManager;
import edu.gsgp.nodes.Node;
import edu.gsgp.normalization.NormalizationStrategy;
import edu.gsgp.population.Individual;
import edu.gsgp.population.fitness.Fitness;
import java.math.BigInteger;

/**
 * @author Luiz Otavio Vilas Boas Oliveira
 * http://homepages.dcc.ufmg.br/~luizvbo/ 
 * luiz.vbo@gmail.com
 * Copyright (C) 20014, Federal University of Minas Gerais, Belo Horizonte, Brazil
 */
public class GSMBreeder extends Breeder{

    public GSMBreeder(PropertiesManager properties, Double probability) {
        super(properties, probability);
    }
    
    private Fitness evaluate(Individual ind, 
                             Node randomTree1,
                             Node randomTree2, 
                             ExperimentalData expData){
        Fitness fitnessFunction = ind.getFitnessFunction().softClone();
        
        NormalizationStrategy normalizer1 = properties.getNormalizationStrategy();
        normalizer1.setup(expData.getDataset(DatasetType.TRAINING), randomTree1);
        
        NormalizationStrategy normalizer2 = properties.getNormalizationStrategy();
        normalizer2.setup(expData.getDataset(DatasetType.TRAINING), randomTree2);
       
        for(DatasetType dataType : DatasetType.values()){
            // Compute the (training/test) semantics of generated random tree
            fitnessFunction.resetFitness(dataType, expData);
            Dataset dataset = expData.getDataset(dataType);
            double[] semInd;
            if(dataType == DatasetType.TRAINING)
                semInd = ind.getTrainingSemantics();
            else 
                semInd =  ind.getTestSemantics();
            int instanceIndex = 0;
                        
            for (Instance instance : dataset) {
                double rtValue = normalizer1.normalize(instance);
                rtValue -= normalizer2.normalize(instance);
                double estimated = semInd[instanceIndex] + properties.getMutationStep() * rtValue;
                fitnessFunction.setSemanticsAtIndex(estimated, instance.output, instanceIndex++, dataType);
            }
            fitnessFunction.computeFitness(dataType);
        }
        return fitnessFunction;
    }

    @Override
    public Individual generateIndividual(MersenneTwister rndGenerator, ExperimentalData expData) {
        Individual p = (Individual)properties.selectIndividual(originalPopulation, rndGenerator);
        Node rt1 = properties.getRandomTree(rndGenerator);
        Node rt2 = properties.getRandomTree(rndGenerator);
        BigInteger numNodes = p.getNumNodes().add(new BigInteger(rt1.getNumNodes()+"")).
                                              add(new BigInteger(rt2.getNumNodes()+"")).
                                              add(BigInteger.ONE);
        Fitness fitnessFunction = evaluate(p, rt1, rt2, expData);
        Individual offspring = new Individual(null, numNodes, fitnessFunction);
        return offspring;
    }
    
    @Override
    public Breeder softClone(PropertiesManager properties) {
        return new GSMBreeder(properties, probability);
    }
}
