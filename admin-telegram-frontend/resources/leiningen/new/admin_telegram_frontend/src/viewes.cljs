(ns {{name}}.viewes
  (:require
   [reagent.core :as reagent]
   [{{name}}.db :refer [app-state]]
   [reagent.ratom :as ratom]
   [{{name}}.pages.units.units :refer [units_page]]
   [{{name}}.pages.model-unit.model-unit :refer [model_unit_page]]
   [{{name}}.pages.product.product :refer [product_page]]
   [{{name}}.pages.new-unit.new-unit :refer [new_unit_page]]
   [{{name}}.pages.orders-history.orders-history :refer [orders_history]]
   [{{name}}.pages.order.order :refer [order_page]]
   [{{name}}.pages.managers.managers :refer [managers_page]]
   [{{name}}.pages.settings.settings :refer [settings_page]]
   [{{name}}.pages.payment-methods.payment-methods :refer [payment_methods_page]]
   [{{name}}.pages.delivery-methods.delivery-methods :refer [delivery_methods_page]]
   )
  )


(defmulti current-page #(@app-state :page))


(defmethod current-page :units []
  [units_page])

(defmethod current-page :model_unit []
  [model_unit_page])

(defmethod current-page :new_unit []
  [new_unit_page]
  )

(defmethod current-page :product []
  [product_page]
  )

(defmethod current-page :orders_history []
  [orders_history]
  )

(defmethod current-page :order []
  [order_page]
  )

(defmethod current-page :managers []
  [managers_page] 
  )

(defmethod current-page :settings []
  [settings_page]
  )

(defmethod current-page :payment_methods []
  [payment_methods_page]
  )

(defmethod current-page :delivery_methods []
  [delivery_methods_page]
  )
