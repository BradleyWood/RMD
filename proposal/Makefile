
default: target/$(PROJECT).pdf

display: default
	(${PDFVIEWER} target/$(PROJECT).pdf &)


### Compilation Flags
PDFLATEX_FLAGS  = -halt-on-error -output-directory target/

TEXINPUTS = .:target/
TEXMFOUTPUT = target/


### File Types (for dependancies)
TEX_FILES = $(shell find . -name '*.tex' -or -name '*.sty' -or -name '*.cls')
BIB_FILES = $(shell find . -name '*.bib')
BST_FILES = $(shell find . -name '*.bst')
IMG_FILES = $(shell find . -path '*.jpg' -or -path '*.png' -or \( \! -path './target/*.pdf' -path '*.pdf' \) )


UNAME := $(shell uname)

ifeq ($(UNAME), Linux)
PDFVIEWER = evince
endif

ifeq ($(UNAME), Darwin)
PDFVIEWER = open
endif

clean::
	rm -rf target/

target/:
	mkdir -p target/

target/$(PROJECT).aux: $(TEX_FILES) $(IMG_FILES) | target/
	xelatex $(TEX_FILES) $(PDFLATEX_FLAGS) $(PROJECT)

target/$(PROJECT).bbl: $(BIB_FILES) | target/$(PROJECT).aux
	bibtex target/$(PROJECT)
	xelatex $(TEX_FILES) $(PDFLATEX_FLAGS) $(PROJECT)
	
target/$(PROJECT).pdf: target/$(PROJECT).aux $(if $(BIB_FILES), target/$(PROJECT).bbl)
	xelatex $(TEX_FILES) $(PDFLATEX_FLAGS) $(PROJECT)
