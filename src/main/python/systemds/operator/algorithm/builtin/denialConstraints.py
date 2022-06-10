# -------------------------------------------------------------
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# -------------------------------------------------------------

# Autogenerated By   : src/main/python/generator/generator.py
# Autogenerated From : scripts/builtin/denialConstraints.dml

from typing import Dict, Iterable

from systemds.operator import OperationNode, Matrix, Frame, List, MultiReturn, Scalar
from systemds.script_building.dag import OutputType
from systemds.utils.consts import VALID_INPUT_TYPES


def denialConstraints(dataFrame: Frame,
                      constraintsFrame: Frame):
    """
     This function considers some constraints indicating statements that can NOT happen in the data (denial constraints).
    
     .. code-block:: txt
    
       EXAMPLE:
       dataFrame:
    
            rank       discipline   yrs.since.phd   yrs.service   sex      salary
       1    Prof       B            19              18            Male     139750
       2    Prof       B            20              16            Male     173200
       3    AsstProf   B            3               3             Male     79750.56
       4    Prof       B            45              39            Male     115000
       5    Prof       B            40              40            Male     141500
       6    AssocProf  B            6               6             Male     97000
       7    Prof       B            30              23            Male     175000
       8    Prof       B            45              45            Male     147765
       9    Prof       B            21              20            Male     119250
       10   Prof       B            18              18            Female   129000
       11   AssocProf  B            12              8             Male     119800
       12   AsstProf   B            7               2             Male     79800
       13   AsstProf   B            1               1             Male     77700
    
       constraintsFrame:
          
       idx   constraint.type   group.by   group.variable      group.option   variable1      relation   variable2
       1     variableCompare   FALSE                                         yrs.since.phd  <          yrs.service
       2     instanceCompare   TRUE       rank                Prof           yrs.service    ><         salary
       3     valueCompare      FALSE                                         salary         =          78182
       4     variableCompare   TRUE       discipline          B              yrs.service    >          yrs.since.phd
    
    
     Example: explanation of constraint 2 --> it can't happen that one professor of rank Prof has more years of service than other, but lower salary.
    
    
    
    :param dataFrame: frame which columns represent the variables of the data and the rows correspond
        to different tuples or instances.
        Recommended to have a column indexing the instances from 1 to N (N=number of instances).
    :param constraintsFrame: frame with fixed columns and each row representing one constraint.
        1. idx: (double) index of the constraint, from 1 to M (number of constraints)
        2. constraint.type: (string) The constraints can be of 3 different kinds:
        - variableCompare: for each instance, it will compare the values of two variables (with a relation <, > or =).
        - valueCompare: for each instance, it will compare a fixed value and a variable value (with a relation <, > or =).
        - instanceCompare: for every couple of instances, it will compare the relation between two variables,
        ie  if the value of the variable 1 in instance 1 is lower/higher than the value of variable 1 in instance 2,
        then the value of of variable 2 in instance 2 can't be lower/higher than the value of variable 2 in instance 2.
        3. group.by: (boolean) if TRUE only one group of data (defined by a variable option) will be considered for the constraint.
        4. group.variable: (string, only if group.by TRUE) name of the variable (column in dataFrame) that will divide our data in groups.
        5. group.option: (only if group.by TRUE) option of the group.variable that defines the group to consider.
        6. variable1: (string) first variable to compare (name of column in dataFrame).
        7. relation: (string) can be < , > or = in the case of variableCompare and valueCompare, and < >, < < , > < or > >
        in the case of instanceCompare
        8. variable2: (string) second variable to compare (name of column in dataFrame) or fixed value for the case of valueCompare.
    :return: Matrix of 2 columns.
        - First column shows the indexes of dataFrame that are wrong.
        - Second column shows the index of the denial constraint that is fulfilled
        If there are no wrong instances to show (0 constrains fulfilled) --> WrongInstances=matrix(0,1,2)
    """

    params_dict = {'dataFrame': dataFrame, 'constraintsFrame': constraintsFrame}
    return Matrix(dataFrame.sds_context,
        'denialConstraints',
        named_input_nodes=params_dict)
