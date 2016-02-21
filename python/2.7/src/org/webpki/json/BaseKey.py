##############################################################################
#                                                                            #
#  Copyright 2006-2015 WebPKI.org (http://webpki.org).                       #
#                                                                            #
#  Licensed under the Apache License, Version 2.0 (the "License");           #
#  you may not use this file except in compliance with the License.          #
#  You may obtain a copy of the License at                                   #
#                                                                            #
#      http://www.apache.org/licenses/LICENSE-2.0                            #
#                                                                            #
#  Unless required by applicable law or agreed to in writing, software       #
#  distributed under the License is distributed on an "AS IS" BASIS,         #
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  #
#  See the License for the specific language governing permissions and       #
#  limitations under the License.                                            #
#                                                                            #
##############################################################################

import abc

####################################################################
# Base class for private key and signature support wrapper classes #
####################################################################

class BaseKey(object):
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def signData(self,data):
        """
        Well, use the private key + hash method and return a signature blob
        """
        return


    @abc.abstractmethod
    def setSignatureMetaData(self,jsonObjectWriter):
        """
        Only for usage by JSONObjectWriter
        """

