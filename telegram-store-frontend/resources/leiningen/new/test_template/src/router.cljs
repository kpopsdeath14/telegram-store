(ns {{name}}.router
  (:import goog.history.Html5History
           goog.Uri)
  (:require
   [goog.events :as e]
   [{{name}}.db :refer [app-state]]
   [secretary.core :as secretary :refer-macros [defroute]]
   [goog.history.EventType :as EventType]

   [reagent.core :as reagent]
   [{{name}}.events.products-get         :refer [products_get]]
   [{{name}}.events.catalog-config-get   :refer [catalog_config_get]] 
   [{{name}}.events.orders-history-get   :refer [orders_history_get]]
   [{{name}}.events.payment-provider-get :refer [payment_provider_get]]
   [{{name}}.events.delivery-provider-get :refer [delivery_provider_get]]
   [{{name}}.events.order-get            :refer [order_get]]
   [{{name}}.events.favorite-get         :refer [favorite_get]]
   )
  )


(set! *warn-on-infer* true)

(defn hook-browser-navigation! []
  (doto (Html5History.)
    (e/listen
     EventType/NAVIGATE
     (fn [^js/Foo.Bar event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))



(defn routes []

  (secretary/set-config! :prefix "#")

  (defroute "/" []
    (swap! app-state assoc :page :catalog)
    (products_get (:search_value @app-state) (:filters_picked @app-state) (:selected_sorting @app-state))
    (catalog_config_get)
    )

  (defroute "/catalog" []
    (swap! app-state assoc :page :catalog)
    (products_get (:search_value @app-state) (:filters_picked @app-state) (:selected_sorting @app-state))
    (catalog_config_get)
    )
  
  (defroute "/product/:unit_id/:product_id" [unit_id product_id]
    (swap! app-state assoc :current_unit_id unit_id)
    (swap! app-state assoc :current_product_id product_id)
    (swap! app-state assoc :page :product) 
    )
  
  (defroute "/information" []
    (swap! app-state assoc :page :information)
    )
  
  (defroute "/cart" []
    (swap! app-state assoc :page :cart)
    )
  
  (defroute "/delivery" []
    (payment_provider_get)
    (delivery_provider_get)
    (swap! app-state assoc :page :delivery)
    )
  
  (defroute "/orders-history" []
    (orders_history_get)
    (swap! app-state assoc :page :orders-history)
    )

  (defroute "/favorites" []
    (favorite_get)
    (swap! app-state assoc :page :favorites)
    )
  
  (defroute "/order/:order_id" [order_id]
    (payment_provider_get)
    (delivery_provider_get)
    (order_get order_id)
    (swap! app-state assoc :page :order)
    )

  (defroute "/thank-you/:order_id" [order_id]
    (payment_provider_get)
    (delivery_provider_get)
    (swap! app-state assoc
           :page :thank-you
           :current_order_id order_id
           :last_paid_order_id order_id)
    (order_get order_id))
  
  orders_history_get

  (hook-browser-navigation!))
