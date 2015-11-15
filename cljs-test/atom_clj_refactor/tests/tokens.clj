(ns atom-clj-refactor.tests.tokens
  (:require [clojure.test :refer [deftest is]]
            [atom-clj-refactor.tokens :as t]))

(deftest scope-parent
  (and (is (t/parent-scope? ["source.clojure"]
                            ["source.clojure"
                             "invalid.trailing-whitespace"]))
       (is (t/parent-scope? ["source.clojure"]
                            ["source.clojure"
                             "meta.expression.clojure"
                             "meta.definition.global.clojure"]))
       (is (not (t/parent-scope? ["source.clojure"
                                  "invalid.trailing-whitespace"]
                                 ["source.clojure"])))))

(deftest scope-sibling
  (and (is (t/sibling-scope? ["source.clojure"]
                             ["source.clojure"]))
       (is (t/sibling-scope?  ["source.clojure"
                               "invalid.trailing-whitespace"]
                              ["source.clojure"
                               "invalid.trailing-whitespace"]))
       (is (t/sibling-scope? ["source.clojure"
                              "meta.expression.clojure"
                              "meta.definition.global.clojure"
                              "entity.global.clojure"]
                             ["source.clojure"
                              "meta.expression.clojure"
                              "meta.definition.global.clojure"
                              "entity.global.clojure"]))
       (is (not (t/sibling-scope? ["source.clojure"]
                                  ["source.clojure"
                                   "invalid.trailing-whitespace"])))))

(deftest token-parent
  (and (is (t/parent-token? {:value "["
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "meta.definition.global.clojure"
                                      "meta.expression.clojure"
                                      "meta.vector.clojure"]}
                            {:value "clojure.pprint"
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "meta.definition.global.clojure"
                                      "meta.expression.clojure"
                                      "meta.vector.clojure"
                                      "meta.symbol.clojure"]}))
       (is (not (t/parent-token? {:value "["
                                  :scopes ["source.clojure"
                                           "meta.expression.clojure"]}
                                 {:value "hello"
                                  :scopes ["source.clojure"
                                           "meta.defition.global.clojure"]})))))

(deftest token-sibling
  (and (is (t/sibling-token? {:value "["
                              :scopes ["source.clojure"
                                       "meta.expression.clojure"
                                       "meta.definition.global.clojure"
                                       "meta.expression.clojure"
                                       "meta.vector.clojure"]}
                             {:value ":hello"
                              :scopes ["source.clojure"
                                       "meta.expression.clojure"
                                       "meta.definition.global.clojure"
                                       "meta.expression.clojure"
                                       "constant.keyword.clojure"]}))
       (is (not (t/sibling-token? {:value "["
                                   :scopes ["source.clojure"
                                            "meta.expression.clojure"
                                            "meta.definition.global.clojure"
                                            "meta.expression.clojure"
                                            "meta.vector.clojure"]}
                                  {:value "clojure.pprint"
                                   :scopes ["source.clojure"
                                            "meta.expression.clojure"
                                            "meta.definition.global.clojure"
                                            "meta.expression.clojure"
                                            "meta.vector.clojure"
                                            "meta.symbol.clojure"]})))))

(deftest find-global-def
  (is (= {:value "foo" :scopes ["source.clojure"
                                "meta.expression.clojure"
                                "meta.definition.global.clojure"
                                "entity.global.clojure"]}
         (t/find-global-def [{:value ")"
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "punctuation.section.expression.end.clojure"]}
                            {:value "("
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "punctuation.section.expression.begin.clojure"]}
                            {:value "defn"
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "meta.definition.global.clojure"
                                      "keyword.control.clojure"]}
                            {:value "foo"
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "meta.definition.global.clojure"
                                      "entity.global.clojure"]}
                            {:value "["
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "meta.definition.global.clojure"
                                      "meta.vector.clojure"]}
                            {:value "]"
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "meta.definition.global.clojure"
                                      "meta.vector.clojure"]}
                            {:value "("
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "meta.definition.global.clojure"
                                      "meta.expression.clojure"
                                      "punctuation.section.expression.begin.clojure"]}
                            {:value "somewhere"
                             :scopes ["source.clojure"
                                      "meta.expression.clojure"
                                      "meta.definition.global.clojure"
                                      "meta.expression.clojure"
                                      "entity.name.function.clojure"]}]))))
