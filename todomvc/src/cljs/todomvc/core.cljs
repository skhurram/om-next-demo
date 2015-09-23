(ns todomvc.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [secretary.macros :refer [defroute]])
  (:require [goog.events :as events]
            [goog.dom :as gdom]
            [secretary.core :as secretary]
            [cljs.core.async :refer [put! <! chan]]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [todomvc.util :as util :refer [hidden pluralize]]
            [todomvc.item :as item])
  (:import [goog History]
           [goog.history EventType]))

;; -----------------------------------------------------------------------------
;; Components

(defn main [{:keys [todos showing editing] :as app}]
  (dom/section #js {:id "main" :style (hidden (empty? todos))}
    (dom/input
      #js {:id       "toggle-all"
           :type     "checkbox"
           :onChange #(toggle-all % app)
           :checked  (every? :completed todos)})
    (apply dom/ul #js {:id "todo-list"} todos)))

(defn clear-button [completed]
  (when (pos? completed)
    (dom/button
      #js {:id "clear-completed"
           :onClick #(do %)}
      (str "Clear completed (" completed ")"))))

(defn footer [app count completed]
  (dom/footer #js {:id "footer" :style (hidden (empty? (:todos/list app)))}
    (dom/span #js {:id "todo-count"}
      (dom/strong nil count)
      (str " " (pluralize count "item") " left"))
    (apply dom/ul #js {:id "filters" :className (name (:showing app))}
      (map (fn [[x y]] (dom/li nil (dom/a #js {:href (str "#/" x)} y)))
        [["" "All"] ["active" "Active"] ["completed" "Completed"]]))
    (clear-button completed)))

(defui Todos
  static om/IQueryParams
  (params [this]
    {:todo-item (om/get-query item/TodoItem)})

  static om/IQuery
  (query [this]
    '[{:todos/list ?todo-item}])

  Object
  (render [this]
    (let [{:keys [todos] :as app} {:todos [] :showing :all :editing nil}
          active (count (remove :completed todos))
          completed (- (count todos) active)]
      (dom/div nil
        (dom/header #js {:id "header"}
          (dom/h1 nil "todos")
          (dom/input
            #js {:ref "newField"
                 :id "new-todo"
                 :placeholder "What needs to be done?"
                 :onKeyDown #(do %)})
          (main app)
          (footer app active completed))))))

(def todos (om/create-factory Todos))

(def reconciler
  (om/reconciler
    {:state  (atom {})
     :parser (om/parser
               {:read (fn [env k params] {:quote true})
                :mutate (fn [env k params] {:quote true})})
     :send   (util/transit-post "/api")}))

(om/add-root! reconciler (gdom/getElement "todoapp") Todos)

(comment
  (go (println (<! (util/transit-post-chan "/api" (om/get-query Todos)))))

  (def p
    (om/parser
      {:read (fn [env k params] {:quote true})
       :mutate (fn [env k params] {:quote true})}))

  (p {} (om/get-query Todos) true)
  )