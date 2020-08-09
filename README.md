# WESSTEC

This is an implementation of the WESSTEC model. The repository contains the java code (labeled data generation module) as well as python code (Wide&Deep module). We will also publish trained embedding models as well as the datasets. Due to the double-blind policy of ISWC, we have published the code under an anonymous user name.

Upon publication, we will provide the GitHub link for the final version of the paper. Also, we will prepare a much more elaborate README file. 

## How to run the code
**Running inside Eclipse**
This project is based on [Gradle](https://gradle.org/). So it could be easily imported to Eclipse. For importing it the Eclipse should contain [Buildship Plugin](https://projects.eclipse.org/projects/tools.buildship).  After installing [Buildship Plugin](https://projects.eclipse.org/projects/tools.buildship), you can easily import the project into the Eclipse as a Gradle project.

## Components of WESSTEC
WESSTEC consists of two main modules: 
(1) a data labeling module, which computes probabilistic labels for a given unlabeled training data set, and 
(2) a classification model based on a Wide & Deep learning approach.

## (1) Labeled Data Generation
This module aims to generate labeled documents from a given label list and unlabeled set of documents. To generate labeled data user should run:
`/src/main/java/org/fiz/ise/gwifi/WESSTEC/LabeledDataGeneration.java` 
The output of this class is labeled documents which can be leveraged for the categorization task.

## (2) Wide&Deep model
To perform the short text categorization task with the Wide&Deep model, users should run: `keras_wide_deep.py`
