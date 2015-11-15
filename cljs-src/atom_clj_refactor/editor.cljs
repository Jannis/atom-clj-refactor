(ns atom-clj-refactor.editor
  (:require [clojure.walk :refer [keywordize-keys]]))

;;;; Positions and ranges

(defn point->vec [p]
  "Converts an Atom position to a vector."
  (vector (.-row p) (.-column p)))

;;;; Editor access

(defn active-editor []
  "Returns the active Atom text editor"
  (js/atom.workspace.getActiveTextEditor))

;;;; Cursors

(defn cursors [editor]
  "Returns the buffer positions of all active cursors in an editor."
  (into []
        (map point->vec)
        (array-seq (.getCursorBufferPositions editor))))

;;;; Tokenizing

(defn annotate-line-token
  "Adds :position meta data to a token."
  [res token]
  (let [token (-> token
                keywordize-keys
                (vary-meta assoc :position (:position res)))]
    (-> res
      (update :tokens conj token)
      (update-in [:position 1] + (count (:value token))))))

(defn annotate-line
  "Adds :position meta data to all tokens of a line"
  [line tokens]
  (:tokens (reduce annotate-line-token
                   {:position [line 0] :tokens []}
                   tokens)))

(defn tokenize-lines
  "Tokenizes all lines based on the given grammar and annotates the
  resulting tokens with :position meta data."
  [grammar lines]
  (->> lines
    (.tokenizeLines grammar)
    js->clj
    (map-indexed annotate-line)
    flatten
    (into [])))

(defn tokenize-text
  "Tokenizes the entire text in the editor based on the active grammar."
  [editor]
  (tokenize-lines (.getGrammar editor)
                  (.getText editor)))

;;;; Scopes

(defn in-scope?
  "Tests whether the token is in the given scope."
  [scope token]
  (some #{scope} (:scopes token)))

(defn tokens-in-scope
  "Returns all tokens in the given scope, relative to a given position."
  [tokens pos scope]
  (letfn [(before-pos? [t]
            (let [t-pos (:position (meta t))]
              (or (< (first t-pos) (first pos))
                  (and (= (first t-pos) (first pos))
                       (<= (second t-pos) (second pos))))))]
    (let [[before after] (split-with before-pos? tokens)]
      (concat (reverse (take-while #(in-scope? scope %) (reverse before)))
              (take-while #(in-scope? scope %) after)))))

;;;; Token lookup

(defn has-scope?
  "Tests whether the deepest scope of the token matches the given scope."
  [token scope]
  (= scope (last (:scopes token))))

(defn has-value?
  "Tests whether the token has the given text value."
  [token value]
  (= value (:value token)))

(defn find-tokens
  "Finds all tokens that match the given predicate."
  [tokens pred]
  (filter pred tokens))

(defn find-token
  "Finds the first token that matches the given predicate."
  [tokens pred]
  (first (find-tokens tokens pred)))

(defn find-block
  "Finds the block with the given scope that surrounds the first token
  matching the given predicate."
  [tokens pred scope]
  (let [match (find-token tokens pred)]
    (tokens-in-scope tokens (:position (meta match)) scope)))

(defn find-blocks
  "Finds the blocks with the given scope that surround all tokens matching
  the given predicate."
  [tokens pred scope]
  (let [matches (find-tokens tokens pred)]
    (map (fn [match]
           (tokens-in-scope tokens
                            (:position (meta match))
                            scope))
         matches)))

;;;; Text manipulation

(defn insert-after-block
  "Inserts the text after the given tokens."
  [editor tokens text]
  (let [pos (:position (meta (last tokens)))]
    (println "insert at" pos text)
    (.. editor getBuffer (insert (clj->js pos) text))))
