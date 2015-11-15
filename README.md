# atom-clj-refactor package for Atom

The goal of this package is to provide the same Clojure refactoring
functionality in Atom that [clj-refactor] adds to Emacs.

## State of development

**The package is work in progress and largely unfinished.**

So far the following refactoring commands are implemented:

* `Add declaration` - adds a `declare` for the current top-level form either
  below any current `declare` statements or below the `ns` block.

[clj-refactor]: (https://github.com/clojure-emacs/clj-refactor.el/wiki)

## Copyright

&copy; 2015 Jannis Pohlmann

Licensed under the MIT License.
