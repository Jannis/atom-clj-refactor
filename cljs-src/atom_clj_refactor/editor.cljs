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
        (comp (map point->vec)
              (map #(update % 1 dec)))
        (array-seq (.getCursorBufferPositions editor))))

;;;; Tokenizing

(defprotocol IToken
  (position [this])
  (value [this])
  (scopes [this]))

(defrecord Token [position value scopes]
  IToken
  (position [this]
    (:position this))
  (value [this]
    (:value this))
  (scopes [this]
    (:scopes this)))

(defn parse-line-token [res atoken]
  (let [token (Token. (:position (meta res))
                      (atoken "value")
                      (atoken "scopes"))]
    (-> res
      (conj token)
      (vary-meta update-in [:position 1] + (count (value token))))))

(defn parse-line [line atokens]
  (reduce parse-line-token
          (vary-meta [] assoc :position [line 0])
          atokens))

(defn tokenize-lines
  "Tokenizes all lines based on the given grammar and annotates the
  resulting tokens with position data."
  [grammar lines]
  (->> lines
    (.tokenizeLines grammar)
    js->clj
    (map-indexed parse-line)
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
  (some #{scope} (scopes token)))

(defn get-scope
  "Returns all tokens in the given scope, relative to a given position."
  [tokens pos scope]
  {:pre [(vector? pos) (= 2 (count pos)) (string? scope)]}
  (letfn [(before-pos? [t]
            (let [t-pos (position t)]
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
  {:pre [(satisfies? IToken token)]}
  (= scope (last (scopes token))))

(defn has-value?
  "Tests whether the token has the given text value."
  [token v]
  {:pre [(satisfies? IToken token)]}
  (= v (value token)))

(defn value-in?
  "Tests whether the value of the token is one of coll."
  [token coll]
  {:pre [(satisfies? IToken token)]}
  (some #{(value token)} coll))

(defn find-first
  "Returns the first token that matches the predicate."
  [tokens pred]
  (first (filter pred tokens)))

(defn find-token
  "Jumps to the first token that matches the predicate."
  [pred tokens]
  (drop-while (complement pred) tokens))

(defn find-scope
  "Jumps to the first token that is in the given scope; returns this
  token and all subsequent tokens that are also in this scope."
  [tokens scope]
  (->> tokens
    (find-token #(in-scope? scope %))
    (filter #(in-scope? scope %))))

(defn find-block
  "Finds the block with the given scope that surrounds the first token
  matching the given predicate."
  [tokens pred scope]
  (let [match (first (find-token pred tokens))]
    (get-scope tokens (position match) scope)))

(defn find-blocks
  "Finds the blocks with the given scope that surround all tokens matching
  the given predicate."
  [tokens pred scope]
  (let [matches (filter pred tokens)]
    (->> matches
      (map position)
      (map #(get-scope tokens % scope)))))

;;;; Text manipulation

(defn insert-after-block
  "Inserts the text after the given tokens."
  [editor tokens text]
  (let [pos (position (last tokens))]
    (.. editor getBuffer (insert (clj->js pos) text))))

(defn insert-after-token
  "Inserts the text after the given token."
  [editor token text]
  (let [pos (update (position token) 1 + (count (value token)))]
    (.. editor getBuffer (insert (clj->js pos) text))))

(defn replace-token
  "Replaces a token with the given text."
  [editor token text]
  (let [start (position token)
        end (update start 1 + (count (value token)))]
    (.. editor getBuffer (setTextInRange (clj->js [start end]) text))))

(defn replace-tokens
  "Replaces a tokens with the given text."
  [editor tokens text]
  (let [start (position (first tokens))
        end (update (position (last tokens)) 1 + (count (value (last tokens))))]
    (.. editor getBuffer (setTextInRange (clj->js [start end]) text))))

(defn delete-token
  "Deletes the given token."
  [editor token]
  (replace-token editor token ""))

(defn delete-tokens
  "Deletes the given tokens."
  [editor tokens]
  (replace-tokens editor tokens ""))
