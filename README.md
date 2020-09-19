# Wikipedia Category Hierarchy Knowledge Graph

This repository contains the source code to create knowledge graph from Wikipedia category hierarchy and experiments of the kgswc paper (review pending).

Directory structure of this repo:

```bash
├── README.md
├── data
│   └── experiments
├── java
│   ├── pom.xml
│   └── src
└── python
    └── statistics.ipynb
```

## Source code

Source code is in `java/` directory. 

## Building Knowledge graph

Source code to build the Wiki KG is in package: `wiki.creation` inside `java/src` directory.
Resultant Wiki knowledge graph is stored in https://osf.io/3wbyr/

The kgswc paper details the knowledge graph building process. A short introduction is described at: https://mdkzaman.com/knowledge-graph-from-wikipedia-category-hierarchy/

## Experimental result 

Experimental results is in `data/experiments` directory.


## Running non-cyclic evaluation experiment
Using String similarity (Levenshtein distance 0) we found 22 concepts which exists in all 3 KGs namely non-cyclic Wiki-KG (created), DBpedia schema and SUMO ontology.

We find the super-class of these concepts and check whether the subclass-superclass relation exist on the non-cyclic Wiki-KG compared to the other 2 KGs.

Running this experiment is straightforward.

### Steps:

1. Find all the concepts on the 3 KGs, and 
2. Find the concepts which exist on all 3 KGs.
3. Find the parents of those concepts.
4. Check either the parents of those concepts are similar (semantically same meaning) in Wiki-KG compared to DBpedia and SUMO.

All resultant files are in `data/experiments/eval_non_cyclic_wkg_vs_dbpedia_and_sumo` directory. 


## Running xai evaluation experiment
We took the motivation from paper http://ceur-ws.org/Vol-2003/NeSy17_paper4.pdf.

Data: All images of mountain, market, workroom and warehouse from the ADE20K dataset are taken.

### Steps:
1. Ontology from images are made by mapping image attributes with wiki_concept, wiki_page and sumo_concept.
    Procedure: https://github.com/md-k-sarker/ecii/wiki/Create-Ontology-or-Knowledge-Graph

2. Mapping: These small ontologies are mapped with WKG, SUMO and DBpedia schema.
    Procedure: https://github.com/md-k-sarker/ecii/wiki/Combine-Ontology

3. Stripping:
    Procedure: https://github.com/md-k-sarker/ecii/wiki/Strip-down-ontology

4. Running program: 
    Accuracy was calculated using efficient concept induction algorithm, following the paper: https://www.aaai.org/ojs/index.php/AAAI/article/view/4161/4039

All owls corresponding to images and configuration files and the results are in `data/experiments/eval_xai_ADE20K_wkg_vs_sumo` directory. 
Configurations files can be used to reproduce the results.
