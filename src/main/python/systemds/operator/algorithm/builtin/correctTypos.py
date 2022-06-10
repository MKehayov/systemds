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
# Autogenerated From : scripts/builtin/correctTypos.dml

from typing import Dict, Iterable

from systemds.operator import OperationNode, Matrix, Frame, List, MultiReturn, Scalar
from systemds.script_building.dag import OutputType
from systemds.utils.consts import VALID_INPUT_TYPES


def correctTypos(strings: Frame,
                 **kwargs: Dict[str, VALID_INPUT_TYPES]):
    """
     Corrects corrupted frames of strings
     This algorithm operates on the assumption that most strings are correct
     and simply swaps strings that do not occur often with similar strings that 
     occur more often
    
     .. code-block:: txt
    
       References:
       Fred J. Damerau. 1964. 
         A technique for computer detection and correction of spelling errors. 
         Commun. ACM 7, 3 (March 1964), 171–176. 
         DOI:https://doi.org/10.1145/363958.363994
    
    
    
    :param strings: The nx1 input frame of corrupted strings
    :param frequency_threshold: Strings that occur above this frequency level will not be corrected
    :param distance_threshold: Max distance at which strings are considered similar
    :param is_verbose: Print debug information
    :return: Corrected nx1 output frame
    """

    params_dict = {'strings': strings}
    params_dict.update(kwargs)
    
    vX_0 = Frame(strings.sds_context, '')
    vX_1 = Scalar(strings.sds_context, '')
    vX_2 = Scalar(strings.sds_context, '')
    vX_3 = Matrix(strings.sds_context, '')
    vX_4 = Frame(strings.sds_context, '')
    output_nodes = [vX_0, vX_1, vX_2, vX_3, vX_4, ]

    op = MultiReturn(strings.sds_context, 'correctTypos', output_nodes, named_input_nodes=params_dict)

    vX_0._unnamed_input_nodes = [op]
    vX_1._unnamed_input_nodes = [op]
    vX_2._unnamed_input_nodes = [op]
    vX_3._unnamed_input_nodes = [op]
    vX_4._unnamed_input_nodes = [op]

    return op
