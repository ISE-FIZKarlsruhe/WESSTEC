#!/usr/bin/env python
# coding: utf-8

# In[4]:
from numpy.random import seed
seed(1)

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
import multiprocessing
import pandas as pd
import numpy as np
import pickle
from keras.preprocessing.text import Tokenizer
from keras.models import Sequential
from keras.layers import Activation, Dense, Dropout
from keras.preprocessing.text import Tokenizer
from keras.models import Sequential
from keras.layers import Activation, Dense, Dropout
from sklearn.preprocessing import LabelBinarizer
import sklearn.datasets as skds
from pathlib import Path
import glob
import tensorflow as tf
from tensorflow import keras
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn import preprocessing
import tensorflow_hub as hub
from sklearn.utils import shuffle
import inspect
import time
from keras.layers import Input, Dense, concatenate
from keras.models import Model
from termcolor import colored

from keras import backend as K
import tensorflow as tf

main_path='/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/data-web-snippets/snippets_joint_features_with_ent_cooc_majority_vote_label/web_snippets_'

path_train_samples=main_path+'train_joint_features_samples'
path_train_label=main_path+'train_joint_features_labels'
path_train_entCooc=main_path+'train_joint_features_entCooc'

path_train_entVec=main_path+'train_joint_features_entVecMean'
path_train_catVec=main_path+'train_joint_features_catVecMean'


path_test_samples=main_path+'test_joint_features_samples'
path_test_label=main_path+'test_joint_features_labels'
path_test_entCooc=main_path+'test_joint_features_entCooc'

path_test_entVec=main_path+'test_joint_features_entVecMean'
path_test_catVec=main_path+'test_joint_features_catVecMean'


list_lines_train_sentences = [line.rstrip('\n') for line in open(path_train_samples)]
list_lines_test_sentences = [line.rstrip('\n') for line in open(path_test_samples)]

print('a sample from training set:' + list_lines_train_sentences[0])
print('a sample from test set:' + list_lines_test_sentences[0])

list_lines_train_entCooc = [line.rstrip('\n') for line in open(path_train_entCooc)]
list_lines_test_entCooc = [line.rstrip('\n') for line in open(path_test_entCooc)]

y_train=np.loadtxt(path_train_label,delimiter=',')
y_test=np.loadtxt(path_test_label,delimiter=',')

print('a sample from y_train:' + str(y_train[0]))
print('a sample from y_test:' + str(y_test[0]))

train_size = int(len(list_lines_train_entCooc))
test_size = int(len(list_lines_test_sentences))
print('train_sentences'+str(train_size))
print('test_sentences'+str(test_size))

#print('train_cats'+str(len(list_lines_train_cats)))
#print('test_cats'+str(len(list_lines_test_cats)))
num_of_epoch=5
num_of_iteration=1
batch_sizes=[32,64,128,256,512] #32,
batch_size_general=64 #32,


#batch_size=32 #32 64 128 256 512
# Create a tokenizer to preprocess our text descriptions
#vocab_size = 15000 # This is a hyperparameter, experiment with different values for your dataset
#tokenize = keras.preprocessing.text.Tokenizer(num_words=vocab_size, char_level=False)
tokenize = keras.preprocessing.text.Tokenizer(filters='\t',char_level=False)
tokenize.fit_on_texts(list_lines_train_entCooc) # only fit on train

description_bow_train = tokenize.texts_to_matrix(list_lines_train_entCooc, mode='binary')
description_bow_test = tokenize.texts_to_matrix(list_lines_test_entCooc, mode='binary')

entity_mean_vec_train=np.loadtxt(path_train_entVec,delimiter=',')
entity_mean_vec_test=np.loadtxt(path_test_entVec,delimiter=',')

category_mean_vec_train=np.loadtxt(path_train_catVec,delimiter=',')
category_mean_vec_test=np.loadtxt(path_test_catVec,delimiter=',')

embed = hub.Module("https://tfhub.dev/google/nnlm-en-dim128/1")
embeddings_train_sentence = embed(list_lines_train_sentences)
embeddings_test_sentence=embed(list_lines_test_sentences)

init = tf.global_variables_initializer()
table_init = tf.tables_initializer()

with tf.Session() as sess:
    sess.run([init, table_init])
    embeddings_train_sentence_ = sess.run(embeddings_train_sentence)
    embeddings_test_sentence_ = sess.run(embeddings_test_sentence)

print('uniqe ent cooc:'+str(description_bow_train.shape[1]))

deep_features_train_cat_ent=np.concatenate((category_mean_vec_train,entity_mean_vec_train),axis=1)
deep_features_test_cat_ent=np.concatenate((category_mean_vec_test,entity_mean_vec_test),axis=1)

deep_features_train_cat_word=np.concatenate((category_mean_vec_train,embeddings_train_sentence_),axis=1)
deep_features_test_cat_word=np.concatenate((category_mean_vec_test,embeddings_test_sentence_),axis=1)

deep_features_train_ent_word=np.concatenate((entity_mean_vec_train,embeddings_train_sentence_),axis=1)
deep_features_test_ent_word=np.concatenate((entity_mean_vec_test,embeddings_test_sentence_),axis=1)

deep_features_train_ent_word_cat=np.concatenate((deep_features_train_ent_word, category_mean_vec_train),axis=1)
deep_features_test_ent_word_cat=np.concatenate((deep_features_test_ent_word,category_mean_vec_test),axis=1)

print('final concatenation: '+str(deep_features_train_ent_word_cat.shape)) 

result_folder='Results/results_snippest_ent_cooc/'

'''
#f = open(result_folder+"results_snippest_linear_entCooc"+str(batch_size_general)+".txt", "w")
for i in range(1):
    print(colored('wide with entCooc results batch size:'+str(batch_size_general), 'red'))
    #print(colored(batch_size_general, 'red'))
    wide_inp = keras.layers.Input(shape=(description_bow_train.shape[1],),  name='wide_inp')
    w = keras.layers.Dense(8, activation='softmax')(wide_inp)
    wide = keras.Model(wide_inp, w)
    wide.compile(loss='categorical_crossentropy',
              optimizer='adam',
              metrics=['accuracy'])
    wide.fit(description_bow_train, y_train, nb_epoch=num_of_epoch, batch_size=batch_size_general)
    print('fit description_bow_train:' + str(description_bow_train[0]))
    result=wide.evaluate(description_bow_test , y_test)
    print('evaluate y_test:' + str(y_test[0]))
    print('test loss, test acc:', result)
    #f.write(str(i)+' '+str(batch_size_general)+': ')
    #f.write(" ".join(str(item) for item in result))
    #f.write('\n')
#f.close()

print(colored('********finished wide*********',  'green'))


#f = open(result_folder+'result_dnn_snippets_theSame_epoch_'+str(num_of_epoch)+".txt", "w")
def dnn_iterate(embedding_train, embedding_test, file_name):
    print("Start dnn with "+str(file_name))
    #f.write(file_name+"-->\n")
    for i in range(1):  
        deep = keras.layers.Input(shape=(embedding_train.shape[1],), name='input_deep')
        deep_dense_1 = keras.layers.Dense(500, activation='relu', name='deep_dense_1')(deep)
        deep_dense_2 = keras.layers.Dense(100, activation='relu', name='deep_dense_2')(deep_dense_1)
        deep_dense_3 = keras.layers.Dense(50, activation='relu', name='deep_dense_3')(deep_dense_2)
        last = keras.layers.Dense(8, activation='softmax', name='deep_model')(deep_dense_3)
        deep_model = keras.Model(inputs=deep, outputs=last)

        deep_model.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['accuracy'])
        deep_model.fit(embedding_train, y_train, nb_epoch=num_of_epoch, batch_size=batch_size_general)
        result=deep_model.evaluate(embedding_test, y_test)
        print('******deep with embedding results*****')
        print('test loss, test acc:', result)
        #f.write(str(i)+' iteration, '+ ' total epochs'+ str(num_of_epoch) +', batch '+str(batch_size_general)+':'+'\t')
        #f.write("\t".join(str(item) for item in result))
        #f.write('\n')
    #f.write('\n\n')
   '''

feature_embeddings_train=[category_mean_vec_train, 
entity_mean_vec_train,
embeddings_train_sentence_,
deep_features_train_cat_ent, 
deep_features_train_cat_word, 
deep_features_train_ent_word, 
deep_features_train_ent_word_cat]

feature_embeddings_test=[category_mean_vec_test, 
entity_mean_vec_test,
embeddings_test_sentence_,
deep_features_test_cat_ent,
deep_features_test_cat_word, 
deep_features_test_ent_word,  
deep_features_test_ent_word_cat]

file_names=[result_folder+'results_snippest_cat_mean_vec', 
result_folder+'results_snippest_entity_mean_vec', 
result_folder+'results_snippest_word_mean_vec',
result_folder+'results_snippest_cat_ent_mean_vec', 
result_folder+'results_snippest_cat_word_vec', 
result_folder+'results_snippest_ent_word_vec',
result_folder+'results_snippest_ent_word_cat_vec']
'''
#for i in range(0, len(file_names)):
for i in range(6,7):
    split = file_names[i].split("/")
    #f.write(str(split[len(split)-1])+"\n")
    #f.write('dnn_batch size:'+str(batch_size_general)+"\n")
    print(colored('iteration '+ str(i)+'-'+str(len(file_names)), 'red'))
    print(colored('feature_embeddings_train[i]: '+str(file_names[i]), 'red'))
    print(colored('feature_embeddings_test[i]: '+str(file_names[i]), 'red'))
    print(colored('dnn_batch size:'+str(batch_size_general), 'red'))
    dnn_iterate(feature_embeddings_train[i], feature_embeddings_test[i], file_names[i])
    print(colored('********dnn*******',  'green'))

 
#f.close()
'''
#f = open(result_folder+'result_wide_and_deep_snippets_diff_features_theSame_epoch_'+str(num_of_epoch)+".txt", "w")
def wide_and_deep(embedding_train, embedding_test, batch_size):
    print('running ', batch_size)
    print('num_of_epoch ', num_of_epoch)
    
    deep = Input(shape=(embedding_train.shape[1],), name='input_deep')
    wide = Input(shape=(description_bow_train.shape[1],), name='input_wide')
    
    for i in range(1):
        deep_dense_1 = Dense(500, activation='relu', name='deep_dense_1')(deep)
        deep_dense_2 = Dense(100, activation='relu', name='deep_dense_2')(deep_dense_1)
        deep_dense_3 = Dense(50, activation='relu', name='deep_dense_3')(deep_dense_2)

        wide_deep = concatenate([deep_dense_3, wide])

        lay = Dense(8, activation='softmax', name='wide_deep')(wide_deep)

        end = Model(inputs=[deep, wide], outputs=lay)
        end.compile(loss='categorical_crossentropy',
              optimizer='adam',
              metrics=['accuracy'])
        end.fit([embedding_train, description_bow_train],y_train, epochs=num_of_epoch, batch_size=batch_size, shuffle=True, verbose=0)
        result=end.evaluate([embedding_test, description_bow_test],y_test)
        
        print('test loss, test acc:', result)
        #f.write(str(i)+' iteration, '+ ' total epochs'+ str(num_of_epoch) +', batch '+str(batch_size)+': ')
        #f.write(" ".join(str(item) for item in result))
        #f.write('\n')
    #f.write('\n\n')
    

#for i in range(6, len(file_names)):
for i in range(6, 7):    
    split = file_names[i].split("/")
    #f.write(str(split[len(split)-1])+"\n")
   # f.write('wide_deep_batch size:'+str(batch_size_general)+"\n")
    print(colored('iteration '+ str(i)+'-'+str(len(file_names)), 'red'))
    print(colored('feature_embeddings_train[i]: '+str(file_names[i]), 'red'))
    print(colored('feature_embeddings_test[i]: '+str(file_names[i]), 'red'))
    print(colored('wide_deep_batch size:'+str(batch_size_general), 'red'))
    wide_and_deep(feature_embeddings_train[i], feature_embeddings_test[i], batch_size_general)
    print(colored('********wide_deep*********',  'green'))

#f.close()    

   
